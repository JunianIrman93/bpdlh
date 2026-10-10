import type { Clock } from '../../../shared/clock.js';
import type { NonceStore } from '../ports/index.js';

export interface IssueChallengeInput {
  deviceId: string;
}

export interface IssueChallengeResult {
  nonce: string;
  expiresIn: number;
}

export type IssueChallenge = (input: IssueChallengeInput) => Promise<IssueChallengeResult>;

export function createIssueChallenge(deps: { nonces: NonceStore; clock: Clock; ttlSeconds: number }): IssueChallenge {
  return async (input) => ({
    nonce: deps.nonces.issue(input.deviceId, deps.clock.now()),
    expiresIn: deps.ttlSeconds,
  });
}
