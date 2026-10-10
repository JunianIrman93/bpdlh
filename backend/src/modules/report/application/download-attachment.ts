import type { Readable } from 'node:stream';
import { AppError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { FileStorage } from '../../../shared/storage/file-storage.js';
import type { ReportRepo } from '../ports/index.js';

export interface DownloadedFile {
  stream: Readable;
  fileName: string;
}

export type DownloadAttachment = (
  actor: Principal,
  reportId: string,
  attachmentId: string,
) => Promise<DownloadedFile>;

export function createDownloadAttachment(deps: { reports: ReportRepo; storage: FileStorage }): DownloadAttachment {
  return async (actor, reportId, attachmentId) => {
    const report = await deps.reports.findById(reportId);
    if (!report || (actor.role === 'FIELD_OFFICER' && report.userId !== actor.userId)) {
      throw new AppError('REPORT_NOT_FOUND', 'Laporan tidak ditemukan');
    }
    const attachment = report.attachments.find((a) => a.id === attachmentId);
    if (!attachment || attachment.state !== 'UPLOADED') {
      throw new AppError('ATTACHMENT_NOT_FOUND', 'Lampiran tidak ditemukan');
    }
    return { stream: await deps.storage.get(attachment.storageKey), fileName: attachment.fileName };
  };
}
