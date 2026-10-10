import AdminJS from 'adminjs';
import { componentLoader } from './components.js';

await new AdminJS({ componentLoader }).initialize();
