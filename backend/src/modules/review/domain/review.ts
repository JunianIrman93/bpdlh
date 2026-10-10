import type { ErrorDetail } from '../../../shared/errors.js';

export type Decision = 'VERIFIED' | 'REJECTED';

export const NOTE_MIN = 5;
export const NOTE_MAX = 500;

export function validateDecision(decision: Decision, note: string | null): ErrorDetail[] {
  const trimmed = note?.trim() ?? '';
  if (decision === 'REJECTED' && trimmed.length === 0) {
    return [{ field: 'note', issue: 'required', message: 'Catatan wajib diisi saat menolak' }];
  }
  if (trimmed.length > 0 && (trimmed.length < NOTE_MIN || trimmed.length > NOTE_MAX)) {
    return [{ field: 'note', issue: 'length', message: `Catatan ${NOTE_MIN} sampai ${NOTE_MAX} karakter` }];
  }
  return [];
}
