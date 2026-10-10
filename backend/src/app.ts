import express, { type Express, type Router } from 'express';
import type { Logger } from 'pino';
import { errorHandler, notFound, sendError } from './shared/http/errors.js';
import { perMinute } from './shared/http/rate-limit.js';
import { requestContext } from './shared/http/request-context.js';

export interface AppDeps {
  logger: Logger;
  checkReady: () => Promise<void>;
  apiRouters: Router[];
  admin?: { path: string; router: Router };
  trustProxy: number;
}

export function createApp(deps: AppDeps): Express {
  const app = express();
  app.disable('x-powered-by');
  app.set('trust proxy', deps.trustProxy);
  app.use(requestContext(deps.logger));

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });
  app.get('/ready', async (_req, res) => {
    try {
      await deps.checkReady();
      res.json({ status: 'ok' });
    } catch (err) {
      deps.logger.warn({ err }, 'readiness check failed');
      sendError(res, 'SERVICE_UNAVAILABLE', 'Server belum siap');
    }
  });

  if (deps.admin) {
    app.post(`${deps.admin.path}/login`, perMinute(10));
    app.use(deps.admin.path, deps.admin.router);
  }

  const api = express.Router();
  api.use(express.json({ limit: '1mb' }));
  for (const router of deps.apiRouters) api.use(router);
  app.use('/api/v1', api);

  app.use(notFound);
  app.use(errorHandler(deps.logger));
  return app;
}
