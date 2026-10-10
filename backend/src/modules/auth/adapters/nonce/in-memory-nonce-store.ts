import { randomBytes } from 'node:crypto';
import type { NonceStore } from '../../ports/index.js';

export function createInMemoryNonceStore(ttlSeconds: number): NonceStore {
  const nonces = new Map<string, { deviceId: string; expiresAt: number }>();

  return {
    issue(deviceId, now) {
      const nowMs = now.getTime();
      for (const [key, entry] of nonces) if (entry.expiresAt <= nowMs) nonces.delete(key);

      const nonce = randomBytes(32).toString('base64url');
      nonces.set(nonce, { deviceId, expiresAt: nowMs + ttlSeconds * 1000 });
      return nonce;
    },

    consume(deviceId, nonce, now) {
      const entry = nonces.get(nonce);
      nonces.delete(nonce);
      return entry !== undefined && entry.deviceId === deviceId && entry.expiresAt > now.getTime();
    },
  };
}
