import { randomUUID } from 'node:crypto';
import { rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { Router, type Request, type RequestHandler, type Response } from 'express';
import multer from 'multer';
import { z } from 'zod';
import type { Clock } from '../../../../shared/clock.js';
import { validationError } from '../../../../shared/errors.js';
import { principalOf } from '../../../../shared/http/auth.js';
import { parse } from '../../../../shared/http/validate.js';
import type { DownloadAttachment } from '../../application/download-attachment.js';
import type { GetReport } from '../../application/get-report.js';
import type { ListReports } from '../../application/list-reports.js';
import type { ReceiveFile, UploadAttachment } from '../../application/upload-attachment.js';
import type { UpsertReport } from '../../application/upsert-report.js';
import { ALLOWED_MIME_TYPES, ATTACHMENT_KINDS, CATEGORIES, REPORT_STATUSES, type ReportContent } from '../../domain/report.js';
import { toDetailDto, toReceiptDto, toSummaryDto } from './presenters.js';

const sha256Hex = z.string().regex(/^[0-9a-fA-F]{64}$/).transform((v) => v.toLowerCase());
const isoDateTime = z.iso.datetime({ offset: true }).transform((v) => new Date(v));

const reportBody = z.object({
  title: z.string().min(5).max(120),
  description: z.string().min(20).max(2000),
  category: z.enum(CATEGORIES),
  activity_date: z.iso.date(),
  location: z.object({
    latitude: z.number().min(-90).max(90),
    longitude: z.number().min(-180).max(180),
    accuracy_m: z.number().min(0),
    captured_at: isoDateTime,
    is_mock: z.boolean(),
  }),
  client_created_at: isoDateTime,
  attachments: z
    .array(
      z.object({
        id: z.uuid(),
        kind: z.enum(ATTACHMENT_KINDS),
        file_name: z.string().min(1).max(255),
        mime_type: z.enum(ALLOWED_MIME_TYPES as [string, ...string[]]),
        size_bytes: z.number().int().min(1),
        sha256: sha256Hex,
      }),
    )
    .min(1)
    .max(10),
});

const reportParams = z.object({ id: z.uuid() });
const attachmentParams = z.object({ id: z.uuid(), attachmentId: z.uuid() });

const listQuery = z.object({
  updated_since: isoDateTime.optional(),
  limit: z.coerce.number().int().min(1).max(100).default(50),
  cursor: z.string().min(1).max(512).optional(),
  status: z.enum(REPORT_STATUSES).optional(),
});

export function reportRouter(deps: {
  upsertReport: UpsertReport;
  uploadAttachment: UploadAttachment;
  listReports: ListReports;
  getReport: GetReport;
  downloadAttachment: DownloadAttachment;
  requireAuth: RequestHandler;
  clock: Clock;
}): Router {
  const router = Router();

  router.get('/reports', deps.requireAuth, async (req, res) => {
    const query = parse(listQuery, req.query);
    const result = await deps.listReports(principalOf(res), {
      status: query.status,
      updatedSince: query.updated_since,
      cursor: query.cursor,
      limit: query.limit,
    });
    res.json({
      items: result.items.map(toSummaryDto),
      next_cursor: result.nextCursor,
      server_time: deps.clock.now().toISOString(),
    });
  });

  router.put('/reports/:id', deps.requireAuth, async (req, res) => {
    const { id } = parse(reportParams, req.params);
    const content = toContent(parse(reportBody, req.body));
    const result = await deps.upsertReport(principalOf(res), id, content);
    res.status(result.created ? 201 : 200).json(toReceiptDto(result.report, deps.clock.now()));
  });

  router.get('/reports/:id', deps.requireAuth, async (req, res) => {
    const { id } = parse(reportParams, req.params);
    const report = await deps.getReport(principalOf(res), id);
    res.json(toDetailDto(report));
  });

  router.put('/reports/:id/attachments/:attachmentId', deps.requireAuth, async (req, res) => {
    const { id, attachmentId } = parse(attachmentParams, req.params);
    const declaredSha256 = parse(sha256Hex, req.get('X-Content-SHA256') ?? '');
    const result = await deps.uploadAttachment({
      actor: principalOf(res),
      reportId: id,
      attachmentId,
      declaredSha256,
      receive: multipartReceiver(req, res),
    });
    res.status(result.created ? 201 : 200).json({
      id: result.attachmentId,
      state: 'UPLOADED',
      report_status: result.reportStatus,
    });
  });

  router.get('/reports/:id/attachments/:attachmentId/file', deps.requireAuth, async (req, res) => {
    const { id, attachmentId } = parse(attachmentParams, req.params);
    const file = await deps.downloadAttachment(principalOf(res), id, attachmentId);
    res.set('X-Content-Type-Options', 'nosniff');
    res.set('Content-Disposition', contentDisposition(file.fileName));
    res.type('application/octet-stream');
    file.stream.pipe(res);
  });

  return router;
}

function contentDisposition(fileName: string): string {
  const ascii = fileName.replace(/[^\x20-\x7e]/g, '_').replace(/"/g, "'");
  return `attachment; filename="${ascii}"; filename*=UTF-8''${encodeURIComponent(fileName)}`;
}

function toContent(body: z.infer<typeof reportBody>): ReportContent {
  return {
    title: body.title,
    description: body.description,
    category: body.category,
    activityDate: body.activity_date,
    location: {
      latitude: body.location.latitude,
      longitude: body.location.longitude,
      accuracyM: body.location.accuracy_m,
      capturedAt: body.location.captured_at,
      isMock: body.location.is_mock,
    },
    clientCreatedAt: body.client_created_at,
    attachments: body.attachments.map((a) => ({
      id: a.id,
      kind: a.kind,
      fileName: a.file_name,
      mimeType: a.mime_type,
      sizeBytes: a.size_bytes,
      sha256: a.sha256,
    })),
  };
}

const diskStorage = multer.diskStorage({
  destination: tmpdir(),
  filename: (_req, _file, cb) => cb(null, `laporlh-upload-${randomUUID()}`),
});

function multipartReceiver(req: Request, res: Response): ReceiveFile {
  return (maxBytes) =>
    new Promise((resolve, reject) => {
      const upload = multer({ storage: diskStorage, limits: { fileSize: maxBytes, files: 1, fields: 0 } });
      upload.single('file')(req, res, (err: unknown) => {
        if (err) {
          reject(err);
          return;
        }
        const file = req.file;
        if (!file) {
          reject(validationError([{ field: 'file', issue: 'required', message: 'Berkas wajib dikirim' }]));
          return;
        }
        resolve({ path: file.path, size: file.size, cleanup: () => rm(file.path, { force: true }) });
      });
    });
}
