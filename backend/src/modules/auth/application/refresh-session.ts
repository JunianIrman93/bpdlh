import type { Clock } from '../../../shared/clock.js';
import { AppError } from '../../../shared/errors.js';
import type { DeviceKeyRepo, DeviceKeyVerifier, NonceStore, TokenIssuer, UserRepo } from '../ports/index.js';

export interface RefreshSessionInput {
  deviceId: string;
  nonce: string;
  signature: string;
}

export interface RefreshSessionResult {
  accessToken: string;
  expiresIn: number;
  deviceExpiresIn: number;
}

export type RefreshSession = (input: RefreshSessionInput) => Promise<RefreshSessionResult>;

export function createRefreshSession(deps: {
  nonces: NonceStore;
  deviceKeys: DeviceKeyRepo;
  keyVerifier: DeviceKeyVerifier;
  users: UserRepo;
  tokens: TokenIssuer;
  clock: Clock;
  accessTtlSeconds: number;
}): RefreshSession {
  return async (input) => {
    const now = deps.clock.now();
    if (!deps.nonces.consume(input.deviceId, input.nonce, now)) {
      throw new AppError('NONCE_INVALID', 'Nonce tidak dikenal, kedaluwarsa, atau sudah dipakai');
    }

    const key = await deps.deviceKeys.findByDeviceId(input.deviceId);
    if (!key || key.revokedAt || key.expiresAt <= now) {
      throw new AppError('DEVICE_KEY_INVALID', 'Kunci perangkat tidak dikenal, dicabut, atau kedaluwarsa');
    }

    const message = `laporlh-auth-v1\n${input.deviceId}\n${input.nonce}`;
    if (!deps.keyVerifier.verifySignature(key.publicKey, message, input.signature)) {
      throw new AppError('DEVICE_KEY_INVALID', 'Tanda tangan tidak valid');
    }

    const user = await deps.users.findById(key.userId);
    if (!user) throw new AppError('DEVICE_KEY_INVALID', 'Kunci perangkat tidak dikenal, dicabut, atau kedaluwarsa');

    await deps.deviceKeys.touchLastUsed(key.id, now);
    const accessToken = await deps.tokens.issue({ userId: user.id, role: user.role, sessionId: key.id });

    return {
      accessToken,
      expiresIn: deps.accessTtlSeconds,
      deviceExpiresIn: Math.floor((key.expiresAt.getTime() - now.getTime()) / 1000),
    };
  };
}
