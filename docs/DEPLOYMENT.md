# deployment notes

Operational detail that does not belong in the main README.

## Image layout

| Image | Base | Size (approx) | Notes |
| --- | --- | --- | --- |
| `ecom/backend` | `eclipse-temurin:17-jre` | 430 MB | multi-stage, dependencies cached in their own layer |
| `ecom/admin-web` | `nginx:1.27-alpine` | 68 MB | static bundle only |
| `ecom/app-mini` | `nginx:1.27-alpine` | 50 MB | static bundle only |
| `ecom/app-web` | `nginx:1.27-alpine` | 52 MB | static bundle only |

The backend Dockerfile copies the POMs before the sources so `dependency:go-offline`
is cached independently of code changes. A backend rebuild after a code edit takes
~40 s once the dependency layer exists; a from-scratch build takes ~13 min because
Maven must fetch the whole Spring Boot tree.

### Base image mirrors

The Dockerfiles reference `docker.m.daocloud.io` for the Maven, Temurin and Node
images. That mirror is used because it resolved reliably from the build host; on a
machine with unrestricted access to Docker Hub, replace the prefixes with the plain
upstream names:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
FROM eclipse-temurin:17-jre
FROM node:20-alpine
FROM nginx:1.27-alpine
```

`eclipse-temurin:17-jre-alpine` is **not** usable here — it has no arm64 manifest,
so an Apple Silicon host fails to pull it. The Debian-based `17-jre` tag is used
instead and is multi-arch.

## Ports

Host ports are all parameterised through `.env`:

```env
API_PORT=8080
ADMIN_UI_PORT=8081
APP_MINI_PORT=8082
APP_WEB_PORT=8083
DB_PORT=5432
REDIS_PORT=6379
```

If a port is taken, change it in `.env` and `docker compose up -d` again. Only the
ports matter; nothing inside the containers changes.

## Health checks

Every service defines a health check, and `backend` waits for both databases to be
healthy before starting:

```yaml
depends_on:
  postgres:
    condition: service_healthy
  redis:
    condition: service_healthy
```

`/actuator/health` is the backend probe. `docker compose ps` reflects it in the
STATUS column.

## Data volumes

| Volume | Contents | Safe to delete? |
| --- | --- | --- |
| `ecom-postgres-data` | all business data | destroys everything |
| `ecom-redis-data` | cache, rate limits, token blacklist | yes, rebuilt lazily |
| `ecom-uploads` | files uploaded through the admin console | no, user content |

`docker compose down` keeps volumes; `down -v` deletes them.

## Flyway

Migrations run on every backend boot and are idempotent. `baseline-on-migrate` is
enabled so an existing database with data is adopted at the current version rather
than failing.

Never edit an applied migration — add `V3__...` instead. After editing a *seed*
migration during development, recreate the volume:

```bash
docker compose down -v && docker compose up -d
```

## Resource limits

The compose file does not pin CPU/memory so it runs anywhere. For a small production
host, these are sensible starting points:

```yaml
backend:
  deploy:
    resources:
      limits: { cpus: '2', memory: 1G }
postgres:
  deploy:
    resources:
      limits: { memory: 1G }
redis:
  command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

Redis already caps memory at 256 MB with `allkeys-lru` in the compose command.

## Reverse proxy / TLS

Terminate TLS in front of the stack (Caddy, Traefik, or a cloud load balancer) and
forward to `ADMIN_UI_PORT` / `APP_MINI_PORT` / `APP_WEB_PORT` / `API_PORT`. The nginx configs inside
the frontend containers set `X-Forwarded-For` and `X-Forwarded-Proto`, so backend
logs and rate limiting see real client IPs.

Three hostnames are typical:

```
admin.example.com   -> :8081
m.example.com       -> :8082   mobile H5 / mini program webview
www.example.com     -> :8083   desktop storefront
```

All three serve `/api` through their own nginx, so the browser stays same-origin
and no CORS preflight is triggered. If you instead terminate TLS centrally and
route by path, point `/api` at `API_PORT` directly.

## Scheduled jobs

| Job | Interval | Behaviour |
| --- | --- | --- |
| close unpaid orders | 5 min | cancels orders older than 45 min past creation, restores stock, releases coupon |
| auto-complete shipped | 60 min | confirms receipt 7 days after shipping |

Tunable via `ecom.jobs.*` in `application.yml`. Both are idempotent; running several
replicas only costs duplicate log lines.

## Verifying a deployment

```bash
curl -s localhost:8080/actuator/health
python3 scripts/smoke_test.py http://localhost:8080
```

The smoke test prints 24 checks and exits non-zero on any failure, so it drops
straight into CI.
