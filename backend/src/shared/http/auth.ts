import type { RequestHandler, Response } from 'express';
import { AppError } from '../errors.js';
import type { Principal } from '../principal.js';

export type VerifyAccessToken = (token: string) => Promise<Principal>;

export function authenticate(verify: VerifyAccessToken): RequestHandler {
  return async (req, res, next) => {
    const header = req.get('Authorization');
    const match = header?.match(/^Bearer ([^\s]+)$/);
    if (!match?.[1]) {
      next(new AppError('UNAUTHENTICATED', 'Butuh access token'));
      return;
    }
    try {
      res.locals.principal = await verify(match[1]);
      next();
    } catch (err) {
      next(err);
    }
  };
}

export function principalOf(res: Response): Principal {
  const principal = res.locals.principal as Principal | undefined;
  if (!principal) throw new AppError('UNAUTHENTICATED', 'Butuh access token');
  return principal;
}
