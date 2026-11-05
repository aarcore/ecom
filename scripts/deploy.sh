#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  One-command launcher for the whole platform.
#
#    ./scripts/deploy.sh            start everything (build images as needed)
#    ./scripts/deploy.sh --rebuild  force a rebuild of all images
#    ./scripts/deploy.sh --reset    wipe data volumes first (destructive!)
#    ./scripts/deploy.sh --logs     tail logs after starting
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."

REBUILD=0
RESET=0
LOGS=0

for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=1 ;;
    --reset)   RESET=1; REBUILD=1 ;;
    --logs)    LOGS=1 ;;
    -h|--help)
      sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker is required but not installed" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "error: docker compose v2 is required" >&2
  exit 1
fi

# --- seed a .env on first run ----------------------------------------------
if [ ! -f .env ]; then
  echo "→ creating .env from .env.example"
  cp .env.example .env
  if command -v openssl >/dev/null 2>&1; then
    SECRET="$(openssl rand -base64 48 | tr -d '\n')"
    # portable in-place edit that works on both GNU and BSD sed
    awk -v s="$SECRET" '/^JWT_SECRET=/ { print "JWT_SECRET=" s; next } { print }' \
      .env > .env.tmp && mv .env.tmp .env
    echo "→ generated a random JWT_SECRET"
  fi
fi

# docker compose reads .env automatically, but this script's own curl calls need
# the same port values, so load them into the shell as well.
set -a
# shellcheck disable=SC1091
. ./.env
set +a

if [ "$RESET" -eq 1 ]; then
  echo "→ removing containers and data volumes"
  docker compose down -v --remove-orphans
fi

if [ "$REBUILD" -eq 1 ]; then
  echo "→ building images"
  docker compose build
fi

echo "→ starting services"
docker compose up -d

echo "→ waiting for the API to become healthy (up to 3 minutes)"
for i in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:${API_PORT:-8080}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    break
  fi
  if [ "$i" -eq 90 ]; then
    echo "error: API did not become healthy in time; run 'docker compose logs backend'" >&2
    exit 1
  fi
  sleep 2
done

cat <<EOF

  ✔ ecom platform is running

    管理端     http://localhost:${ADMIN_UI_PORT:-8081}
    小程序/H5  http://localhost:${APP_MINI_PORT:-8082}
    PC 商城    http://localhost:${APP_WEB_PORT:-8083}
    接口文档   http://localhost:${API_PORT:-8080}/swagger-ui.html

    管理员     admin / admin123
    会员      13800138000 / 123456

    数据库     postgres://${DB_USER:-postgres}@localhost:${DB_PORT:-5432}/${DB_NAME:-ecom}
    缓存       redis://localhost:${REDIS_PORT:-6379}

EOF

if [ "$LOGS" -eq 1 ]; then
  docker compose logs -f
fi
