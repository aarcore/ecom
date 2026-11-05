#!/usr/bin/env bash
# Local development helper: starts only the infrastructure (PostgreSQL + Redis)
# and prints the commands for running the backend and both frontends with hot
# reload. Nothing is containerised except the databases.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "→ starting postgres + redis"
docker compose up -d postgres redis

echo "→ waiting for postgres"
for i in $(seq 1 30); do
  if docker exec ecom-postgres pg_isready -U "${DB_USER:-postgres}" -d "${DB_NAME:-ecom}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

cat <<'EOF'

  Infrastructure ready. Now run each of these in its own terminal:

  1) backend (JDK 17)
     export DB_HOST=localhost DB_PORT=5432 DB_NAME=ecom DB_USER=postgres DB_PASSWORD=postgres
     export REDIS_HOST=localhost REDIS_PORT=6379
     mvn -pl ecom-boot -am spring-boot:run -Dspring-boot.run.profiles=dev

  2) admin console
     cd ecom-admin-web && npm install && npm run dev     # http://localhost:5173

  3) customer client (H5 + mini program webview)
     cd ecom-app-mini && npm install && npm run dev     # http://localhost:5174

  4) customer client (desktop web)
     cd ecom-app-web && npm install && npm run dev      # http://localhost:5175

  Flyway applies migrations and seed data automatically on backend startup.

EOF
