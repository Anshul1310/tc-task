# ──────────────────────────────────────────────
# Dockerfile for the Node.js Backend
#
# Multi-stage build:
#   Stage 1 (deps): Install node_modules
#   Stage 2 (app):  Copy code + run
#
# This keeps the final image smaller because we
# don't include npm cache or build tools.
# ──────────────────────────────────────────────

# Stage 1: Install dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production

# Stage 2: Copy app code and run
FROM node:20-alpine
WORKDIR /app

# Copy dependencies from Stage 1
COPY --from=deps /app/node_modules ./node_modules

# Copy all project files
COPY . .

# Generate Prisma Client
RUN npx prisma generate

# Create uploads directory
RUN mkdir -p src/uploads

# Expose port
EXPOSE 3000

# Start the server
CMD ["node", "src/server.js"]
