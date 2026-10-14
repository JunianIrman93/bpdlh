import { z } from 'zod';

const flag = z.enum(['true', 'false']).transform((v) => v === 'true');
const positiveInt = z.coerce.number().int().positive();
const secret = z.string().min(32);

const schema = z
  .object({
    NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
    PORT: positiveInt.default(8080),
    TRUST_PROXY: z.coerce.number().int().min(0).default(0),
    DATABASE_URL: z.string().min(1),
    JWT_SECRET: secret,
    JWT_ISSUER: z.string().default('laporlh-api'),
    JWT_AUDIENCE: z.string().default('laporlh-mobile'),
    ACCESS_TTL_SECONDS: positiveInt.default(300),
    DEVICE_KEY_TTL_SECONDS: positiveInt.default(2_592_000),
    NONCE_TTL_SECONDS: positiveInt.default(60),
    MAX_UPLOAD_BYTES: positiveInt.default(2_097_152),
    STORAGE_DRIVER: z.enum(['disk', 's3']).default('disk'),
    STORAGE_DISK_PATH: z.string().default('/data/files'),
    S3_ENDPOINT: z.url().optional(),
    S3_REGION: z.string().optional(),
    S3_BUCKET: z.string().optional(),
    S3_ACCESS_KEY: z.string().optional(),
    S3_SECRET_KEY: z.string().optional(),
    S3_FORCE_PATH_STYLE: flag.default(true),
    ADMIN_ENABLED: flag.default(true),
    ADMIN_COOKIE_SECRET: secret.optional(),
    BCRYPT_COST: z.coerce.number().int().min(10).max(14).default(12),
    SEED_ENABLED: flag.default(false),
    SEED_OFFICER_EMAIL: z.email().optional(),
    SEED_OFFICER_PASSWORD: z.string().optional(),
    SEED_REVIEWER_EMAIL: z.email().optional(),
    SEED_REVIEWER_PASSWORD: z.string().optional(),
  })
  .superRefine((env, ctx) => {
    const require = (keys: (keyof typeof env)[], reason: string) => {
      for (const key of keys) {
        if (!env[key]) ctx.addIssue({ code: 'custom', path: [key], message: `wajib bila ${reason}` });
      }
    };
    if (env.STORAGE_DRIVER === 's3') {
      require(['S3_ENDPOINT', 'S3_REGION', 'S3_BUCKET', 'S3_ACCESS_KEY', 'S3_SECRET_KEY'], 'STORAGE_DRIVER=s3');
    }
    if (env.ADMIN_ENABLED) {
      require(['ADMIN_COOKIE_SECRET'], 'ADMIN_ENABLED=true');
    }
    if (env.SEED_ENABLED) {
      require(
        ['SEED_OFFICER_EMAIL', 'SEED_OFFICER_PASSWORD', 'SEED_REVIEWER_EMAIL', 'SEED_REVIEWER_PASSWORD'],
        'SEED_ENABLED=true',
      );
    }
  });

export type Env = z.infer<typeof schema>;

export function loadEnv(source: NodeJS.ProcessEnv = process.env): Env {
  const provided = Object.fromEntries(Object.entries(source).filter(([, value]) => value !== ''));
  const result = schema.safeParse(provided);
  if (!result.success) {
    const lines = result.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`);
    throw new Error(`Konfigurasi tidak valid:\n${lines.join('\n')}`);
  }
  return result.data;
}
