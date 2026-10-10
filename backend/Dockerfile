FROM node:22-bookworm-slim AS base
RUN apt-get update \
  && apt-get install -y --no-install-recommends openssl \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV ADMIN_JS_TMP_DIR=admin-bundle

FROM base AS build
COPY package.json package-lock.json ./
RUN npm ci
COPY prisma ./prisma
RUN npx prisma generate
COPY tsconfig.json tsconfig.build.json ./
COPY src ./src
RUN npm run build \
  && NODE_ENV=production npm run bundle:admin \
  && npm prune --omit=dev \
  && npx prisma generate

FROM base
ENV NODE_ENV=production \
    ADMIN_JS_SKIP_BUNDLE=true \
    PORT=8080
COPY --from=build /app/package.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/dist ./dist
COPY --from=build /app/admin-bundle ./admin-bundle
RUN mkdir -p /data/files && chown node:node /data/files
USER node
EXPOSE 8080
CMD ["sh", "-c", "node_modules/.bin/prisma migrate deploy && exec node dist/main.js"]
