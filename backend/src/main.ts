import { PrismaClient } from '@prisma/client';
import { pino } from 'pino';
import { createApp } from './app.js';
import { createGetProfile } from './modules/auth/application/get-profile.js';
import { createIssueChallenge } from './modules/auth/application/issue-challenge.js';
import { createLogin } from './modules/auth/application/login.js';
import { createLogout } from './modules/auth/application/logout.js';
import { createRefreshSession } from './modules/auth/application/refresh-session.js';
import { seedDemoUsers } from './modules/auth/application/seed-demo-users.js';
import { createVerifyCredentials } from './modules/auth/application/verify-credentials.js';
import { BcryptHasher } from './modules/auth/adapters/crypto/bcrypt-hasher.js';
import { ecP256Verifier } from './modules/auth/adapters/crypto/device-key.js';
import { JwtService } from './modules/auth/adapters/crypto/jwt.js';
import { authRouter } from './modules/auth/adapters/http/router.js';
import { createInMemoryNonceStore } from './modules/auth/adapters/nonce/in-memory-nonce-store.js';
import { PrismaDeviceKeyRepo, PrismaUserRepo } from './modules/auth/adapters/persistence/prisma-repos.js';
import { createDownloadAttachment } from './modules/report/application/download-attachment.js';
import { createGetReport } from './modules/report/application/get-report.js';
import { createListReports } from './modules/report/application/list-reports.js';
import { createUploadAttachment } from './modules/report/application/upload-attachment.js';
import { createUpsertReport } from './modules/report/application/upsert-report.js';
import { inspectFile } from './modules/report/adapters/files/inspect-file.js';
import { toSummaryDto } from './modules/report/adapters/http/presenters.js';
import { reportRouter } from './modules/report/adapters/http/router.js';
import { PrismaReportRepo } from './modules/report/adapters/persistence/prisma-report-repo.js';
import { createReviewReport } from './modules/review/application/review-report.js';
import { ADMIN_ROOT, createAdmin } from './modules/review/adapters/admin/admin.js';
import { reviewRouter } from './modules/review/adapters/http/router.js';
import { systemClock } from './shared/clock.js';
import { loadEnv } from './shared/config/env.js';
import { AppError } from './shared/errors.js';
import { authenticate } from './shared/http/auth.js';
import { DiskStorage } from './shared/storage/disk-storage.js';
import type { FileStorage } from './shared/storage/file-storage.js';
import { S3Storage } from './shared/storage/s3-storage.js';

const env = loadEnv();
const logger = pino({ level: env.NODE_ENV === 'test' ? 'silent' : 'info' });
const clock = systemClock;
const prisma = new PrismaClient();

const storage: FileStorage =
  env.STORAGE_DRIVER === 's3'
    ? new S3Storage({
        endpoint: env.S3_ENDPOINT!,
        region: env.S3_REGION!,
        bucket: env.S3_BUCKET!,
        accessKeyId: env.S3_ACCESS_KEY!,
        secretAccessKey: env.S3_SECRET_KEY!,
        forcePathStyle: env.S3_FORCE_PATH_STYLE,
      })
    : new DiskStorage(env.STORAGE_DISK_PATH);

const users = new PrismaUserRepo(prisma);
const deviceKeys = new PrismaDeviceKeyRepo(prisma);
const reports = new PrismaReportRepo(prisma);
const nonces = createInMemoryNonceStore(env.NONCE_TTL_SECONDS);
const hasher = new BcryptHasher(env.BCRYPT_COST);
const jwt = new JwtService(
  { secret: env.JWT_SECRET, issuer: env.JWT_ISSUER, audience: env.JWT_AUDIENCE, ttlSeconds: env.ACCESS_TTL_SECONDS },
  clock,
);
const verifyCredentials = createVerifyCredentials({ users, hasher });
const requireAuth = authenticate((token) => jwt.verify(token));

const reviewReport = createReviewReport({ reports, clock });

if (env.SEED_ENABLED) {
  const created = await seedDemoUsers({ users, hasher }, [
    {
      email: env.SEED_OFFICER_EMAIL!,
      password: env.SEED_OFFICER_PASSWORD!,
      fullName: 'Petugas Demo',
      role: 'FIELD_OFFICER',
    },
    {
      email: env.SEED_REVIEWER_EMAIL!,
      password: env.SEED_REVIEWER_PASSWORD!,
      fullName: 'Verifikator Demo',
      role: 'REVIEWER',
    },
  ]);
  if (created.length) logger.info({ accounts: created }, 'demo accounts created');
}

const admin = env.ADMIN_ENABLED
  ? createAdmin({
      prisma,
      reviewReport,
      cookieSecret: env.ADMIN_COOKIE_SECRET!,
      authenticateReviewer: async (email, password) => {
        try {
          const user = await verifyCredentials(email, password);
          return user.role === 'REVIEWER' ? { id: user.id, email: user.email, role: user.role } : null;
        } catch (err) {
          if (err instanceof AppError) return null;
          throw err;
        }
      },
    })
  : undefined;

const app = createApp({
  logger,
  trustProxy: env.TRUST_PROXY,
  checkReady: async () => {
    await prisma.$queryRaw`SELECT 1`;
  },
  admin: admin && { path: ADMIN_ROOT, router: admin.router },
  apiRouters: [
    authRouter({
      login: createLogin({
        verifyCredentials,
        deviceKeys,
        keyVerifier: ecP256Verifier,
        tokens: jwt,
        clock,
        accessTtlSeconds: env.ACCESS_TTL_SECONDS,
        deviceKeyTtlSeconds: env.DEVICE_KEY_TTL_SECONDS,
      }),
      issueChallenge: createIssueChallenge({ nonces, clock, ttlSeconds: env.NONCE_TTL_SECONDS }),
      refreshSession: createRefreshSession({
        nonces,
        deviceKeys,
        keyVerifier: ecP256Verifier,
        users,
        tokens: jwt,
        clock,
        accessTtlSeconds: env.ACCESS_TTL_SECONDS,
        deviceKeyTtlSeconds: env.DEVICE_KEY_TTL_SECONDS,
      }),
      logout: createLogout({ deviceKeys, clock }),
      getProfile: createGetProfile({ users }),
      requireAuth,
    }),
    reportRouter({
      upsertReport: createUpsertReport({
        reports,
        storage,
        clock,
        maxFileBytes: env.MAX_UPLOAD_BYTES,
        onStorageError: (err, key) => logger.warn({ err, key }, 'failed to delete stored file'),
      }),
      uploadAttachment: createUploadAttachment({ reports, storage, inspect: inspectFile, clock }),
      listReports: createListReports({ reports }),
      getReport: createGetReport({ reports }),
      downloadAttachment: createDownloadAttachment({ reports, storage }),
      requireAuth,
      clock,
    }),
    reviewRouter({ reviewReport, requireAuth, presentSummary: toSummaryDto }),
  ],
});

const server = app.listen(env.PORT, () => logger.info({ port: env.PORT }, 'listening'));

async function shutdown(signal: string) {
  logger.info({ signal }, 'shutting down');
  server.close();
  await prisma.$disconnect();
  process.exit(0);
}
process.once('SIGTERM', () => void shutdown('SIGTERM'));
process.once('SIGINT', () => void shutdown('SIGINT'));
