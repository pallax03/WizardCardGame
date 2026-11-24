# Stage 1: Build Next.js
FROM node:24-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
COPY application/package.json ./application/
RUN npm ci --workspace=application
COPY application ./application
ENV STANDALONE_BUILD=true
RUN npm run build:client

# Stage 2: Run standalone Next.js
FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
COPY --from=builder --chown=node:node /app/application/.next/standalone ./
COPY --from=builder --chown=node:node /app/application/.next/static ./application/.next/static
COPY --from=builder --chown=node:node /app/application/public ./application/public
USER node
EXPOSE 3000
CMD ["node", "application/server.js"]
