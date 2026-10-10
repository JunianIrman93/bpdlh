import type { Clock } from '../../../shared/clock.js';
import { AppError, validationError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { ReportRecord, ReportRepo } from '../../report/ports/index.js';
import { validateDecision, type Decision } from '../domain/review.js';

export type ReviewReport = (input: {
  actor: Principal;
  reportId: string;
  decision: Decision;
  note: string | null;
}) => Promise<ReportRecord>;

export function createReviewReport(deps: { reports: ReportRepo; clock: Clock }): ReviewReport {
  return async ({ actor, reportId, decision, note }) => {
    if (actor.role !== 'REVIEWER') throw new AppError('FORBIDDEN', 'Hanya verifikator yang dapat meninjau laporan');

    const details = validateDecision(decision, note);
    if (details.length) throw validationError(details);

    const trimmed = note?.trim() || null;
    const updated = await deps.reports.review({
      id: reportId,
      status: decision,
      note: trimmed,
      reviewerId: actor.userId,
      at: deps.clock.now(),
    });
    if (updated) return updated;

    const existing = await deps.reports.findById(reportId);
    if (!existing) throw new AppError('REPORT_NOT_FOUND', 'Laporan tidak ditemukan');
    throw new AppError('INVALID_STATE', 'Hanya laporan berstatus SUBMITTED yang dapat ditinjau');
  };
}
