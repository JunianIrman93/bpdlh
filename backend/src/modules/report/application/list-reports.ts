import { validationError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { ReportStatus } from '../domain/report.js';
import type { ReportRecord, ReportRepo } from '../ports/index.js';

export interface ListReportsInput {
  status?: ReportStatus;
  updatedSince?: Date;
  cursor?: string;
  limit: number;
}

export interface ListReportsResult {
  items: ReportRecord[];
  nextCursor: string | null;
}

export type ListReports = (actor: Principal, input: ListReportsInput) => Promise<ListReportsResult>;

export function createListReports(deps: { reports: ReportRepo }): ListReports {
  return async (actor, input) => {
    const after = input.cursor ? decodeCursor(input.cursor) : undefined;
    const items = await deps.reports.list({
      ownerId: actor.role === 'FIELD_OFFICER' ? actor.userId : null,
      status: input.status,
      updatedSince: input.updatedSince,
      after,
      limit: input.limit,
    });

    const last = items.at(-1);
    const nextCursor = items.length === input.limit && last ? encodeCursor(last) : null;
    return { items, nextCursor };
  };
}

function encodeCursor(item: { updatedAt: Date; id: string }): string {
  return Buffer.from(`${item.updatedAt.toISOString()}|${item.id}`, 'utf8').toString('base64url');
}

function decodeCursor(raw: string): { updatedAt: Date; id: string } {
  const [iso, id] = Buffer.from(raw, 'base64url').toString('utf8').split('|');
  const updatedAt = iso ? new Date(iso) : null;
  if (!updatedAt || Number.isNaN(updatedAt.getTime()) || !id) {
    throw validationError([{ field: 'cursor', issue: 'format', message: 'Kursor tidak valid' }]);
  }
  return { updatedAt, id };
}
