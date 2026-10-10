import js from '@eslint/js';
import tseslint from 'typescript-eslint';

const modules = ['auth', 'report', 'review'];

const boundaries = modules.map((name) => ({
  files: [`src/modules/${name}/**/*.ts`],
  rules: {
    'no-restricted-imports': [
      'error',
      {
        patterns: modules
          .filter((other) => other !== name)
          .map((other) => ({
            group: [`**/modules/${other}/adapters/**`, `**/${other}/adapters/**`],
            message: `Gunakan ports/ milik modul ${other}, bukan adapters/.`,
          })),
      },
    ],
  },
}));

export default tseslint.config(
  { ignores: ['dist/', 'node_modules/'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...boundaries,
);
