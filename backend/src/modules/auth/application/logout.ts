import type { Clock } from '../../../shared/clock.js';
import type { DeviceKeyRepo } from '../ports/index.js';

export interface LogoutInput {
  sessionId: string;
}

export type Logout = (input: LogoutInput) => Promise<void>;

export function createLogout(deps: { deviceKeys: DeviceKeyRepo; clock: Clock }): Logout {
  return async (input) => {
    await deps.deviceKeys.revoke(input.sessionId, deps.clock.now());
  };
}
