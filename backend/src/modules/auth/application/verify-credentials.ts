import { AppError, validationError } from '../../../shared/errors.js';
import { MAX_PASSWORD_BYTES, normalizeEmail, type User } from '../domain/user.js';
import type { PasswordHasher, UserRepo } from '../ports/index.js';

export type VerifyCredentials = (email: string, password: string) => Promise<User>;

export function createVerifyCredentials(deps: { users: UserRepo; hasher: PasswordHasher }): VerifyCredentials {
  let dummyHash: Promise<string> | undefined;

  return async (email, password) => {
    if (Buffer.byteLength(password, 'utf8') > MAX_PASSWORD_BYTES) {
      throw validationError([
        { field: 'password', issue: 'length_max', message: `Maksimal ${MAX_PASSWORD_BYTES} byte` },
      ]);
    }
    const user = await deps.users.findByEmail(normalizeEmail(email));
    if (!user) {
      dummyHash ??= deps.hasher.hash('laporlh-dummy-password');
      await deps.hasher.verify(password, await dummyHash);
      throw invalidCredentials();
    }
    if (!(await deps.hasher.verify(password, user.passwordHash))) throw invalidCredentials();
    return user;
  };
}

function invalidCredentials(): AppError {
  return new AppError('INVALID_CREDENTIALS', 'Surel atau kata sandi salah');
}
