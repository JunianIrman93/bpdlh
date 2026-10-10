import bcrypt from 'bcrypt';
import type { PasswordHasher } from '../../ports/index.js';

export class BcryptHasher implements PasswordHasher {
  constructor(private readonly cost: number) {}

  hash(password: string): Promise<string> {
    return bcrypt.hash(password, this.cost);
  }

  verify(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }
}
