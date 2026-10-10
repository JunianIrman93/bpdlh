import type { DeviceKey as DeviceKeyRow, PrismaClient, User as UserRow } from '@prisma/client';
import type { User } from '../../domain/user.js';
import type { DeviceKeyRecord, DeviceKeyRepo, UserRepo } from '../../ports/index.js';

export class PrismaUserRepo implements UserRepo {
  constructor(private readonly db: PrismaClient) {}

  async findByEmail(email: string): Promise<User | null> {
    const row = await this.db.user.findUnique({ where: { email } });
    return row && toUser(row);
  }

  async findById(id: string): Promise<User | null> {
    const row = await this.db.user.findUnique({ where: { id } });
    return row && toUser(row);
  }

  async create(input: Parameters<UserRepo['create']>[0]): Promise<User> {
    return toUser(await this.db.user.create({ data: input }));
  }
}

export class PrismaDeviceKeyRepo implements DeviceKeyRepo {
  constructor(private readonly db: PrismaClient) {}

  async register(input: Parameters<DeviceKeyRepo['register']>[0]): Promise<{ id: string }> {
    const [, created] = await this.db.$transaction([
      this.db.deviceKey.deleteMany({ where: { deviceId: input.deviceId } }),
      this.db.deviceKey.create({
        data: {
          userId: input.userId,
          deviceId: input.deviceId,
          publicKey: input.publicKey,
          createdAt: input.now,
          lastUsedAt: input.now,
          expiresAt: input.expiresAt,
        },
        select: { id: true },
      }),
    ]);
    return created;
  }

  async findByDeviceId(deviceId: string): Promise<DeviceKeyRecord | null> {
    const row = await this.db.deviceKey.findUnique({ where: { deviceId } });
    return row && toDeviceKeyRecord(row);
  }

  async touchLastUsed(id: string, now: Date): Promise<void> {
    await this.db.deviceKey.update({ where: { id }, data: { lastUsedAt: now } });
  }

  async revoke(id: string, now: Date): Promise<void> {
    await this.db.deviceKey.updateMany({ where: { id, revokedAt: null }, data: { revokedAt: now } });
  }
}

function toUser(row: UserRow): User {
  return {
    id: row.id,
    email: row.email,
    fullName: row.fullName,
    role: row.role,
    passwordHash: row.passwordHash,
  };
}

function toDeviceKeyRecord(row: DeviceKeyRow): DeviceKeyRecord {
  return {
    id: row.id,
    userId: row.userId,
    deviceId: row.deviceId,
    publicKey: row.publicKey,
    expiresAt: row.expiresAt,
    revokedAt: row.revokedAt,
  };
}
