import type { z } from 'zod';
import { validationError, type ErrorDetail } from '../errors.js';

export function parse<T extends z.ZodType>(schema: T, input: unknown): z.infer<T> {
  const result = schema.safeParse(input);
  if (!result.success) throw validationError(result.error.issues.map(toDetail));
  return result.data;
}

function toDetail(issue: z.core.$ZodIssue): ErrorDetail {
  const field = issue.path.join('.') || '(body)';
  switch (issue.code) {
    case 'too_small':
      return issue.origin === 'string'
        ? { field, issue: 'length_min', message: `Minimal ${issue.minimum} karakter` }
        : { field, issue: 'min', message: `Minimal ${issue.minimum}` };
    case 'too_big':
      return issue.origin === 'string'
        ? { field, issue: 'length_max', message: `Maksimal ${issue.maximum} karakter` }
        : { field, issue: 'max', message: `Maksimal ${issue.maximum}` };
    case 'invalid_type':
      return issue.input === undefined
        ? { field, issue: 'required', message: 'Wajib diisi' }
        : { field, issue: 'type', message: 'Jenis nilai tidak sesuai' };
    case 'invalid_format':
      return { field, issue: 'format', message: 'Format tidak valid' };
    case 'invalid_value':
      return { field, issue: 'enum', message: 'Nilai tidak dikenal' };
    case 'unrecognized_keys':
      return { field, issue: 'unknown_field', message: `Isian tidak dikenal: ${issue.keys.join(', ')}` };
    default:
      return { field, issue: 'invalid', message: issue.message };
  }
}
