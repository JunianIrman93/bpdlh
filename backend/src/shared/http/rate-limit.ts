import { rateLimit } from 'express-rate-limit';
import { sendError } from './errors.js';

export function perMinute(limit: number) {
  return rateLimit({
    windowMs: 60_000,
    limit,
    standardHeaders: 'draft-7',
    legacyHeaders: false,
    handler: (_req, res) => sendError(res, 'RATE_LIMITED', 'Terlalu banyak permintaan, coba lagi nanti'),
  });
}
