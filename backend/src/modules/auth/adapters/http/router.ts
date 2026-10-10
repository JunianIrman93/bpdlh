import { Router, type RequestHandler } from 'express';
import { z } from 'zod';
import { principalOf } from '../../../../shared/http/auth.js';
import { perMinute } from '../../../../shared/http/rate-limit.js';
import { parse } from '../../../../shared/http/validate.js';
import type { GetProfile } from '../../application/get-profile.js';
import type { IssueChallenge } from '../../application/issue-challenge.js';
import type { Login } from '../../application/login.js';
import type { Logout } from '../../application/logout.js';
import type { RefreshSession } from '../../application/refresh-session.js';
import type { User } from '../../domain/user.js';

const loginBody = z.object({
  email: z.email().max(254),
  password: z.string().min(1).max(72),
  device_id: z.uuid(),
  device_public_key: z.string().min(1).max(1024),
});

const challengeBody = z.object({
  device_id: z.uuid(),
});

const refreshBody = z.object({
  device_id: z.uuid(),
  nonce: z.string().min(1).max(512),
  signature: z.string().min(1).max(1024),
});

export function authRouter(deps: {
  login: Login;
  issueChallenge: IssueChallenge;
  refreshSession: RefreshSession;
  logout: Logout;
  getProfile: GetProfile;
  requireAuth: RequestHandler;
}): Router {
  const router = Router();

  router.post('/auth/login', perMinute(10), async (req, res) => {
    const body = parse(loginBody, req.body);
    const result = await deps.login({
      email: body.email,
      password: body.password,
      deviceId: body.device_id,
      devicePublicKey: body.device_public_key,
    });
    res.json({
      token_type: 'Bearer',
      access_token: result.accessToken,
      expires_in: result.expiresIn,
      device_expires_in: result.deviceExpiresIn,
      user: toUserDto(result.user),
    });
  });

  router.post('/auth/challenge', perMinute(20), async (req, res) => {
    const body = parse(challengeBody, req.body);
    const result = await deps.issueChallenge({ deviceId: body.device_id });
    res.json({ nonce: result.nonce, expires_in: result.expiresIn });
  });

  router.post('/auth/refresh', perMinute(20), async (req, res) => {
    const body = parse(refreshBody, req.body);
    const result = await deps.refreshSession({
      deviceId: body.device_id,
      nonce: body.nonce,
      signature: body.signature,
    });
    res.json({
      token_type: 'Bearer',
      access_token: result.accessToken,
      expires_in: result.expiresIn,
      device_expires_in: result.deviceExpiresIn,
    });
  });

  router.post('/auth/logout', deps.requireAuth, async (_req, res) => {
    const { sessionId } = principalOf(res);
    if (sessionId) await deps.logout({ sessionId });
    res.status(204).end();
  });

  router.get('/me', deps.requireAuth, async (_req, res) => {
    res.json(toUserDto(await deps.getProfile(principalOf(res))));
  });

  return router;
}

function toUserDto(user: User) {
  return { id: user.id, email: user.email, full_name: user.fullName, role: user.role };
}
