import type { Clock } from '../../../shared/clock.js';
import { validationError } from '../../../shared/errors.js';
import type { User } from '../domain/user.js';
import type { DeviceKeyRepo, DeviceKeyVerifier, TokenIssuer } from '../ports/index.js';
import type { VerifyCredentials } from './verify-credentials.js';

export interface LoginInput {
  email: string;
  password: string;
  deviceId: string;
  devicePublicKey: string;
}

export interface LoginResult {
  accessToken: string;
  expiresIn: number;
  deviceExpiresIn: number;
  user: User;
}

export type Login = (input: LoginInput) => Promise<LoginResult>;

export function createLogin(deps: {
  verifyCredentials: VerifyCredentials;
  deviceKeys: DeviceKeyRepo;
  keyVerifier: DeviceKeyVerifier;
  tokens: TokenIssuer;
  clock: Clock;
  accessTtlSeconds: number;
  deviceKeyTtlSeconds: number;
}): Login {
  return async (input) => {
    if (!deps.keyVerifier.isValidPublicKey(input.devicePublicKey)) {
      throw validationError([
        { field: 'device_public_key', issue: 'format', message: 'Kunci publik harus EC P-256 (SPKI DER, base64)' },
      ]);
    }
    const user = await deps.verifyCredentials(input.email, input.password);

    const now = deps.clock.now();
    const key = await deps.deviceKeys.register({
      userId: user.id,
      deviceId: input.deviceId,
      publicKey: input.devicePublicKey,
      now,
      expiresAt: new Date(now.getTime() + deps.deviceKeyTtlSeconds * 1000),
    });
    const accessToken = await deps.tokens.issue({ userId: user.id, role: user.role, sessionId: key.id });

    return {
      accessToken,
      expiresIn: deps.accessTtlSeconds,
      deviceExpiresIn: deps.deviceKeyTtlSeconds,
      user,
    };
  };
}
