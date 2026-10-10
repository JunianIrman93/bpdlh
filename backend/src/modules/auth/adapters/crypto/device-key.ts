import { createPublicKey, verify } from 'node:crypto';
import type { DeviceKeyVerifier } from '../../ports/index.js';

const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

export const ecP256Verifier: DeviceKeyVerifier = {
  isValidPublicKey(spkiBase64) {
    if (!BASE64.test(spkiBase64)) return false;
    try {
      const key = createPublicKey({ key: Buffer.from(spkiBase64, 'base64'), format: 'der', type: 'spki' });
      return key.asymmetricKeyType === 'ec' && key.asymmetricKeyDetails?.namedCurve === 'prime256v1';
    } catch {
      return false;
    }
  },

  verifySignature(spkiBase64, message, signatureBase64Url) {
    try {
      const key = createPublicKey({ key: Buffer.from(spkiBase64, 'base64'), format: 'der', type: 'spki' });
      const signature = Buffer.from(signatureBase64Url, 'base64url');
      return verify('sha256', Buffer.from(message, 'utf8'), key, signature);
    } catch {
      return false;
    }
  },
};
