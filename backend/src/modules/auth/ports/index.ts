import type { Principal, Role } from '../../../shared/principal.js';
import type { User } from '../domain/user.js';

export interface UserRepo {
  findByEmail(email: string): Promise<User | null>;
  findById(id: string): Promise<User | null>;
  create(input: { email: string; fullName: string; role: Role; passwordHash: string }): Promise<User>;
}

export interface DeviceKeyRecord {
  id: string;
  userId: string;
  deviceId: string;
  publicKey: string;
  expiresAt: Date;
  revokedAt: Date | null;
}

export interface DeviceKeyRepo {
  register(input: {
    userId: string;
    deviceId: string;
    publicKey: string;
    now: Date;
    expiresAt: Date;
  }): Promise<{ id: string }>;
  findByDeviceId(deviceId: string): Promise<DeviceKeyRecord | null>;
  touchLastUsed(id: string, now: Date): Promise<void>;
  revoke(id: string, now: Date): Promise<void>;
}

export interface PasswordHasher {
  hash(password: string): Promise<string>;
  verify(password: string, hash: string): Promise<boolean>;
}

export interface TokenIssuer {
  issue(principal: Principal): Promise<string>;
}

export interface DeviceKeyVerifier {
  isValidPublicKey(spkiBase64: string): boolean;
  verifySignature(spkiBase64: string, message: string, signatureBase64Url: string): boolean;
}

export interface NonceStore {
  issue(deviceId: string, now: Date): string;
  consume(deviceId: string, nonce: string, now: Date): boolean;
}
