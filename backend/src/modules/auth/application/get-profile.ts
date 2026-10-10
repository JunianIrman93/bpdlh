import { AppError } from '../../../shared/errors.js';
import type { Principal } from '../../../shared/principal.js';
import type { User } from '../domain/user.js';
import type { UserRepo } from '../ports/index.js';

export type GetProfile = (actor: Principal) => Promise<User>;

export function createGetProfile(deps: { users: UserRepo }): GetProfile {
  return async (actor) => {
    const user = await deps.users.findById(actor.userId);
    if (!user) throw new AppError('TOKEN_INVALID', 'Sesi tidak valid');
    return user;
  };
}
