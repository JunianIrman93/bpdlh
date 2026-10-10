import type { Role } from '../../../shared/principal.js';
import { MAX_PASSWORD_BYTES, normalizeEmail } from '../domain/user.js';
import type { PasswordHasher, UserRepo } from '../ports/index.js';

export interface DemoAccount {
  email: string;
  password: string;
  fullName: string;
  role: Role;
}

export async function seedDemoUsers(
  deps: { users: UserRepo; hasher: PasswordHasher },
  accounts: DemoAccount[],
): Promise<string[]> {
  const created: string[] = [];
  for (const account of accounts) {
    if (Buffer.byteLength(account.password, 'utf8') > MAX_PASSWORD_BYTES) {
      throw new Error(`Kata sandi akun demo ${account.email} melebihi ${MAX_PASSWORD_BYTES} byte`);
    }
    const email = normalizeEmail(account.email);
    if (await deps.users.findByEmail(email)) continue;
    await deps.users.create({
      email,
      fullName: account.fullName,
      role: account.role,
      passwordHash: await deps.hasher.hash(account.password),
    });
    created.push(email);
  }
  return created;
}
