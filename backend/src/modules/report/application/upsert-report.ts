import type { Clock } from '../../../shared/clock.js';
import { AppError, validationError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { FileStorage } from '../../../shared/storage/file-storage.js';
import {
  decideUpsert,
  IdConflictError,
  jakartaDate,
  validateContent,
  type ReportContent,
  type ReportRecord,
} from '../domain/report.js';
import type { ReportRepo } from '../ports/index.js';

export interface UpsertResult {
  created: boolean;
  report: ReportRecord;
}

export type UpsertReport = (actor: Principal, id: string, content: ReportContent) => Promise<UpsertResult>;

export function createUpsertReport(deps: {
  reports: ReportRepo;
  storage: FileStorage;
  clock: Clock;
  maxFileBytes: number;
  onStorageError: (err: unknown, key: string) => void;
}): UpsertReport {
  return async (actor, id, content) => {
    if (actor.role !== 'FIELD_OFFICER') throw new AppError('FORBIDDEN', 'Hanya petugas yang dapat mengirim laporan');

    const details = validateContent(content, {
      today: jakartaDate(deps.clock.now()),
      maxFileBytes: deps.maxFileBytes,
    });
    if (details.length) throw validationError(details);

    for (let attempt = 1; attempt <= 2; attempt++) {
      const existing = await deps.reports.findById(id);
      const decision = decideUpsert(existing, actor.userId, content);

      if (decision === 'unchanged') return { created: false, report: existing! };

      if (decision === 'create') {
        try {
          return { created: true, report: await deps.reports.create({ id, userId: actor.userId, content }) };
        } catch (err) {
          if (!(err instanceof IdConflictError)) throw err;
          if (err.target === 'attachment') throw attachmentIdTaken();
          continue;
        }
      }

      const result = await resubmit(deps, id, existing!.version, content);
      if (result) return { created: true, report: result };
    }
    throw new AppError('REPORT_LOCKED', 'Laporan sedang diubah oleh permintaan lain');
  };
}

async function resubmit(
  deps: { reports: ReportRepo; storage: FileStorage; onStorageError: (err: unknown, key: string) => void },
  id: string,
  expectedVersion: number,
  content: ReportContent,
): Promise<ReportRecord | null> {
  let result;
  try {
    result = await deps.reports.resubmit({ id, expectedVersion, content });
  } catch (err) {
    throw err instanceof IdConflictError ? attachmentIdTaken() : err;
  }
  if (!result) return null;
  await Promise.all(
    result.removedStorageKeys.map((key) => deps.storage.delete(key).catch((err) => deps.onStorageError(err, key))),
  );
  return result.report;
}

function attachmentIdTaken(): AppError {
  return validationError([
    { field: 'attachments', issue: 'id_taken', message: 'ID lampiran sudah dipakai laporan lain' },
  ]);
}
