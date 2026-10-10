import type { Role } from '../../../shared/principal.js';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  passwordHash: string;
}

export const MAX_PASSWORD_BYTES = 72;

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}
