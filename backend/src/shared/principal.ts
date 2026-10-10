export type Role = 'FIELD_OFFICER' | 'REVIEWER';

export interface Principal {
  userId: string;
  role: Role;
  sessionId?: string;
}
