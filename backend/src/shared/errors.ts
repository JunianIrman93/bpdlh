export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_INVALID'
  | 'INVALID_CREDENTIALS'
  | 'DEVICE_KEY_INVALID'
  | 'NONCE_INVALID'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'REPORT_NOT_FOUND'
  | 'ATTACHMENT_NOT_FOUND'
  | 'REPORT_LOCKED'
  | 'INVALID_STATE'
  | 'PAYLOAD_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'CHECKSUM_MISMATCH'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR'
  | 'SERVICE_UNAVAILABLE';

export interface ErrorDetail {
  field: string;
  issue: string;
  message: string;
}

export class AppError extends Error {
  constructor(
    readonly code: ErrorCode,
    message: string,
    readonly details?: ErrorDetail[],
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export function validationError(details: ErrorDetail[]): AppError {
  return new AppError('VALIDATION_ERROR', 'Isian tidak valid', details);
}
