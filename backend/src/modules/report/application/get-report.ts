import { AppError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { ReportRecord, ReportRepo } from '../ports/index.js';

export type GetReport = (actor: Principal, id: string) => Promise<ReportRecord>;

export function createGetReport(deps: { reports: ReportRepo }): GetReport {
  return async (actor, id) => {
    const report = await deps.reports.findById(id);
    if (!report || (actor.role === 'FIELD_OFFICER' && report.userId !== actor.userId)) {
      throw new AppError('REPORT_NOT_FOUND', 'Laporan tidak ditemukan');
    }
    return report;
  };
}
