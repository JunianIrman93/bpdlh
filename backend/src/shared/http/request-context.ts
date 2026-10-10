import { randomUUID } from 'node:crypto';
import type { RequestHandler, Response } from 'express';
import type { Logger } from 'pino';

const CLIENT_ID_PATTERN = /^[A-Za-z0-9._-]{1,128}$/;

export function requestContext(logger: Logger): RequestHandler {
  return (req, res, next) => {
    const incoming = req.get('X-Request-ID');
    const id = incoming && CLIENT_ID_PATTERN.test(incoming) ? incoming : randomUUID();
    res.locals.requestId = id;
    res.setHeader('X-Request-ID', id);

    const started = process.hrtime.bigint();
    res.on('finish', () => {
      logger.info({
        req_id: id,
        method: req.method,
        path: req.originalUrl.split('?')[0],
        status: res.statusCode,
        ms: Number(process.hrtime.bigint() - started) / 1e6,
      });
    });
    next();
  };
}

export function requestIdOf(res: Response): string {
  return String(res.locals.requestId ?? '');
}
