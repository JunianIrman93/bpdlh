import { Router, type RequestHandler } from 'express';
import { z } from 'zod';
import { principalOf } from '../../../../shared/http/auth.js';
import { parse } from '../../../../shared/http/validate.js';
import type { ReportRecord } from '../../../report/ports/index.js';
import type { ReviewReport } from '../../application/review-report.js';

const reviewBody = z.object({
  decision: z.enum(['VERIFIED', 'REJECTED']),
  note: z.string().max(500).optional(),
});
const params = z.object({ id: z.uuid() });

export function reviewRouter(deps: {
  reviewReport: ReviewReport;
  requireAuth: RequestHandler;
  presentSummary: (report: ReportRecord) => unknown;
}): Router {
  const router = Router();

  router.patch('/reports/:id/review', deps.requireAuth, async (req, res) => {
    const { id } = parse(params, req.params);
    const body = parse(reviewBody, req.body);
    const report = await deps.reviewReport({
      actor: principalOf(res),
      reportId: id,
      decision: body.decision,
      note: body.note ?? null,
    });
    res.json(deps.presentSummary(report));
  });

  return router;
}
