import { fileURLToPath } from 'node:url';
import { ComponentLoader } from 'adminjs';

export const componentLoader = new ComponentLoader();

export const Components = {
  ReviewAction: componentLoader.add('ReviewAction', fileURLToPath(new URL('./review-action', import.meta.url))),
};
