import type { Clock } from '../../../shared/clock.js';
import { AppError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { FileStorage } from '../../../shared/storage/file-storage.js';
import { contentMatches } from '../domain/file-signature.js';
import type { ReportStatus } from '../domain/report.js';
import type { InspectFile, ReportRepo } from '../ports/index.js';

export interface ReceivedFile {
  path: string;
  size: number;
  cleanup(): Promise<void>;
}

export type ReceiveFile = (maxBytes: number) => Promise<ReceivedFile>;

export interface UploadResult {
  created: boolean;
  attachmentId: string;
  reportStatus: ReportStatus;
}

export type UploadAttachment = (input: {
  actor: Principal;
  reportId: string;
  attachmentId: string;
  declaredSha256: string;
  receive: ReceiveFile;
}) => Promise<UploadResult>;

export function createUploadAttachment(deps: {
  reports: ReportRepo;
  storage: FileStorage;
  inspect: InspectFile;
  clock: Clock;
}): UploadAttachment {
  return async ({ actor, reportId, attachmentId, declaredSha256, receive }) => {
    if (actor.role !== 'FIELD_OFFICER') throw new AppError('FORBIDDEN', 'Hanya petugas yang dapat mengunggah lampiran');

    const report = await deps.reports.findById(reportId);
    if (!report || report.userId !== actor.userId) throw new AppError('REPORT_NOT_FOUND', 'Laporan tidak ditemukan');
    const attachment = report.attachments.find((a) => a.id === attachmentId);
    if (!attachment) throw new AppError('ATTACHMENT_NOT_FOUND', 'Lampiran tidak terdaftar pada laporan');

    const file = await receive(attachment.sizeBytes);
    try {
      if (file.size !== attachment.sizeBytes) {
        throw new AppError('PAYLOAD_TOO_LARGE', 'Ukuran berkas tidak sama dengan manifes');
      }
      const inspected = await deps.inspect(file.path);
      if (!contentMatches(attachment.mimeType, inspected.head, inspected.tail)) {
        throw new AppError('UNSUPPORTED_MEDIA_TYPE', 'Isi berkas tidak sesuai tipe pada manifes');
      }
      if (inspected.sha256 !== attachment.sha256 || declaredSha256.toLowerCase() !== attachment.sha256) {
        throw new AppError('CHECKSUM_MISMATCH', 'Checksum berkas tidak sama dengan manifes');
      }

      const created = attachment.state === 'EXPECTED';
      if (created) {
        await deps.storage.put(attachment.storageKey, file.path, attachment.mimeType);
        await deps.reports.markAttachmentUploaded(attachment.id, deps.clock.now());
      }
      const reportStatus = await deps.reports.submitIfComplete(reportId);
      return { created, attachmentId, reportStatus };
    } finally {
      await file.cleanup();
    }
  };
}
