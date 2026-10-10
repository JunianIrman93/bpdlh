import { randomUUID } from 'node:crypto';
import { errors, jwtVerify, SignJWT } from 'jose';
import type { Clock } from '../../../../shared/clock.js';
import { AppError } from '../../../../shared/errors.js';
import type { Principal } from '../../../../shared/principal.js';
import type { TokenIssuer } from '../../ports/index.js';

export interface JwtOptions {
  secret: string;
  issuer: string;
  audience: string;
  ttlSeconds: number;
}

export class JwtService implements TokenIssuer {
  private readonly key: Uint8Array;

  constructor(
    private readonly options: JwtOptions,
    private readonly clock: Clock,
  ) {
    this.key = new TextEncoder().encode(options.secret);
  }

  async issue(principal: Principal): Promise<string> {
    const iat = Math.floor(this.clock.now().getTime() / 1000);
    return new SignJWT({ role: principal.role, sid: principal.sessionId })
      .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
      .setIssuer(this.options.issuer)
      .setAudience(this.options.audience)
      .setSubject(principal.userId)
      .setJti(randomUUID())
      .setIssuedAt(iat)
      .setExpirationTime(iat + this.options.ttlSeconds)
      .sign(this.key);
  }

  async verify(token: string): Promise<Principal> {
    try {
      const { payload } = await jwtVerify(token, this.key, {
        algorithms: ['HS256'],
        issuer: this.options.issuer,
        audience: this.options.audience,
        requiredClaims: ['sub', 'exp', 'iat', 'jti'],
        currentDate: this.clock.now(),
        clockTolerance: 0,
      });
      const { sub, role, sid } = payload;
      if (typeof sub !== 'string' || typeof sid !== 'string' || (role !== 'FIELD_OFFICER' && role !== 'REVIEWER')) {
        throw new AppError('TOKEN_INVALID', 'Token tidak valid');
      }
      return { userId: sub, role, sessionId: sid };
    } catch (err) {
      if (err instanceof AppError) throw err;
      if (err instanceof errors.JWTExpired) throw new AppError('TOKEN_EXPIRED', 'Token kedaluwarsa');
      throw new AppError('TOKEN_INVALID', 'Token tidak valid');
    }
  }
}
