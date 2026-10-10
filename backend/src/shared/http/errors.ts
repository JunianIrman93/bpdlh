import type { ErrorRequestHandler, RequestHandler, Response } from 'express';
import multer from 'multer';
import type { Logger } from 'pino';
import { AppError, type ErrorCode, type ErrorDetail } from '../errors.js';
import { requestIdOf } from './request-context.js';

const STATUS: Record<ErrorCode, number> = {
  VALIDATION_ERROR: 400,
  UNAUTHENTICATED: 401,
  TOKEN_EXPIRED: 401,
  TOKEN_INVALID: 401,
  INVALID_CREDENTIALS: 401,
  DEVICE_KEY_INVALID: 401,
  NONCE_INVALID: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  REPORT_NOT_FOUND: 404,
  ATTACHMENT_NOT_FOUND: 404,
  REPORT_LOCKED: 409,
  INVALID_STATE: 409,
  PAYLOAD_TOO_LARGE: 413,
  UNSUPPORTED_MEDIA_TYPE: 415,
  CHECKSUM_MISMATCH: 422,
  RATE_LIMITED: 429,
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
};

export function sendError(res: Response, code: ErrorCode, message: string, details?: ErrorDetail[]): void {
  res.status(STATUS[code]).json({
    error: { code, message, ...(details?.length ? { details } : {}), request_id: requestIdOf(res) },
  });
}

export const notFound: RequestHandler = (_req, res) => {
  sendError(res, 'NOT_FOUND', 'Alamat tidak ditemukan');
};

export function errorHandler(logger: Logger): ErrorRequestHandler {
  return (err, _req, res, next) => {
    if (res.headersSent) {
      next(err);
      return;
    }
    const known = toAppError(err);
    if (known) {
      sendError(res, known.code, known.message, known.details);
      return;
    }
    logger.error({ req_id: requestIdOf(res), err }, 'unhandled error');
    sendError(res, 'INTERNAL_ERROR', 'Terjadi galat di server');
  };
}

function toAppError(err: unknown): AppError | null {
  if (err instanceof AppError) return err;
  if (err instanceof multer.MulterError) {
    return err.code === 'LIMIT_FILE_SIZE'
      ? new AppError('PAYLOAD_TOO_LARGE', 'Berkas melebihi batas')
      : new AppError('VALIDATION_ERROR', 'Unggahan tidak valid', [
          { field: err.field ?? 'file', issue: 'upload', message: err.message },
        ]);
  }
  const { type, status } = err as { type?: unknown; status?: unknown };
  if (status === 404) return new AppError('NOT_FOUND', 'Alamat tidak ditemukan');
  if (type === 'entity.too.large') return new AppError('PAYLOAD_TOO_LARGE', 'Badan permintaan melebihi batas');
  if (type === 'entity.parse.failed') {
    return new AppError('VALIDATION_ERROR', 'Isian tidak valid', [
      { field: '(body)', issue: 'json', message: 'JSON tidak valid' },
    ]);
  }
  return null;
}
