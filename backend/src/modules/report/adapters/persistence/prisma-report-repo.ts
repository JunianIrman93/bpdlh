import { Prisma, type PrismaClient } from '@prisma/client';
import {
  IdConflictError,
  storageKeyFor,
  type ManifestItem,
  type ReportContent,
  type ReportRecord,
  type ReportStatus,
} from '../../domain/report.js';
import type { ReportListFilter, ReportRepo } from '../../ports/index.js';

const withAttachments = { attachments: { orderBy: { createdAt: 'asc' } } } satisfies Prisma.ReportInclude;
type ReportRow = Prisma.ReportGetPayload<{ include: typeof withAttachments }>;
type Tx = Prisma.TransactionClient;

export class PrismaReportRepo implements ReportRepo {
  constructor(private readonly db: PrismaClient) {}

  async findById(id: string): Promise<ReportRecord | null> {
    return findById(this.db, id);
  }

  async list(filter: ReportListFilter): Promise<ReportRecord[]> {
    const rows = await this.db.report.findMany({
      where: {
        ...(filter.ownerId ? { userId: filter.ownerId } : {}),
        ...(filter.status ? { status: filter.status } : {}),
        ...(filter.updatedSince ? { updatedAt: { gte: filter.updatedSince } } : {}),
        ...(filter.after
          ? {
              OR: [
                { updatedAt: { gt: filter.after.updatedAt } },
                { updatedAt: filter.after.updatedAt, id: { gt: filter.after.id } },
              ],
            }
          : {}),
      },
      orderBy: [{ updatedAt: 'asc' }, { id: 'asc' }],
      take: filter.limit,
      include: withAttachments,
    });
    return rows.map(toRecord);
  }

  async create(input: { id: string; userId: string; content: ReportContent }): Promise<ReportRecord> {
    try {
      const row = await this.db.report.create({
        data: {
          id: input.id,
          userId: input.userId,
          ...contentColumns(input.content),
          attachments: { create: input.content.attachments.map((a) => attachmentColumns(input.id, a)) },
        },
        include: withAttachments,
      });
      return toRecord(row);
    } catch (err) {
      throw asIdConflict(err) ?? err;
    }
  }

  async resubmit(input: {
    id: string;
    expectedVersion: number;
    content: ReportContent;
  }): Promise<{ report: ReportRecord; removedStorageKeys: string[] } | null> {
    try {
      return await this.db.$transaction(async (tx) => {
        const { count } = await tx.report.updateMany({
          where: { id: input.id, status: 'REJECTED', version: input.expectedVersion },
          data: {
            ...contentColumns(input.content),
            status: 'UPLOADING',
            version: { increment: 1 },
            reviewNote: null,
            reviewedById: null,
            reviewedAt: null,
          },
        });
        if (count === 0) return null;

        const previous = new Map(
          (await tx.attachment.findMany({ where: { reportId: input.id } })).map((a) => [a.id, a]),
        );
        const incomingIds = new Set(input.content.attachments.map((a) => a.id));
        const removed = [...previous.values()].filter((a) => !incomingIds.has(a.id));
        await tx.attachment.deleteMany({ where: { id: { in: removed.map((a) => a.id) } } });

        for (const item of input.content.attachments) {
          const prev = previous.get(item.id);
          if (!prev) {
            await tx.attachment.create({ data: { ...attachmentColumns(input.id, item), reportId: input.id } });
          } else if (prev.mimeType === item.mimeType && prev.sizeBytes === item.sizeBytes && prev.sha256 === item.sha256) {
            await tx.attachment.update({ where: { id: item.id }, data: { kind: item.kind, fileName: item.fileName } });
          } else {
            await tx.attachment.update({
              where: { id: item.id },
              data: { ...attachmentColumns(input.id, item), state: 'EXPECTED', uploadedAt: null },
            });
          }
        }

        await submitIfComplete(tx, input.id);
        const report = await findById(tx, input.id);
        return {
          report: report!,
          removedStorageKeys: removed.filter((a) => a.state === 'UPLOADED').map((a) => a.storageKey),
        };
      });
    } catch (err) {
      throw asIdConflict(err) ?? err;
    }
  }

  async markAttachmentUploaded(attachmentId: string, at: Date): Promise<void> {
    await this.db.attachment.updateMany({
      where: { id: attachmentId, state: 'EXPECTED' },
      data: { state: 'UPLOADED', uploadedAt: at },
    });
  }

  async submitIfComplete(reportId: string): Promise<ReportStatus> {
    return submitIfComplete(this.db, reportId);
  }

  async review(input: {
    id: string;
    status: 'VERIFIED' | 'REJECTED';
    note: string | null;
    reviewerId: string;
    at: Date;
  }): Promise<ReportRecord | null> {
    const { count } = await this.db.report.updateMany({
      where: { id: input.id, status: 'SUBMITTED' },
      data: { status: input.status, reviewNote: input.note, reviewedById: input.reviewerId, reviewedAt: input.at },
    });
    return count === 0 ? null : findById(this.db, input.id);
  }
}

async function findById(db: PrismaClient | Tx, id: string): Promise<ReportRecord | null> {
  const row = await db.report.findUnique({ where: { id }, include: withAttachments });
  return row && toRecord(row);
}

async function submitIfComplete(db: PrismaClient | Tx, reportId: string): Promise<ReportStatus> {
  await db.report.updateMany({
    where: { id: reportId, status: 'UPLOADING', attachments: { none: { state: 'EXPECTED' } } },
    data: { status: 'SUBMITTED' },
  });
  const { status } = await db.report.findUniqueOrThrow({ where: { id: reportId }, select: { status: true } });
  return status;
}

function contentColumns(content: ReportContent) {
  return {
    title: content.title,
    description: content.description,
    category: content.category,
    activityDate: new Date(`${content.activityDate}T00:00:00Z`),
    latitude: content.location.latitude,
    longitude: content.location.longitude,
    accuracyM: content.location.accuracyM,
    locationCapturedAt: content.location.capturedAt,
    isMockLocation: content.location.isMock,
    locationAddress: content.location.address,
    clientCreatedAt: content.clientCreatedAt,
  };
}

function attachmentColumns(reportId: string, item: ManifestItem) {
  return {
    id: item.id,
    kind: item.kind,
    fileName: item.fileName,
    mimeType: item.mimeType,
    sizeBytes: item.sizeBytes,
    sha256: item.sha256,
    storageKey: storageKeyFor(reportId, item.id),
  };
}

function toRecord(row: ReportRow): ReportRecord {
  return {
    id: row.id,
    userId: row.userId,
    title: row.title,
    description: row.description,
    category: row.category,
    activityDate: row.activityDate.toISOString().slice(0, 10),
    location: {
      latitude: row.latitude,
      longitude: row.longitude,
      accuracyM: row.accuracyM,
      capturedAt: row.locationCapturedAt,
      isMock: row.isMockLocation,
      address: row.locationAddress,
    },
    clientCreatedAt: row.clientCreatedAt,
    status: row.status,
    reviewNote: row.reviewNote,
    version: row.version,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
    attachments: row.attachments.map((a) => ({
      id: a.id,
      kind: a.kind,
      fileName: a.fileName,
      mimeType: a.mimeType,
      sizeBytes: a.sizeBytes,
      sha256: a.sha256,
      state: a.state,
      storageKey: a.storageKey,
    })),
  };
}

function asIdConflict(err: unknown): IdConflictError | null {
  if (!(err instanceof Prisma.PrismaClientKnownRequestError) || err.code !== 'P2002') return null;
  return new IdConflictError(err.meta?.modelName === 'Attachment' ? 'attachment' : 'report');
}
