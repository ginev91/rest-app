# Backend — Run & Troubleshooting Guide

This document explains how to run the backend services (main-service, kitchen-svc, postgres, redis, jaeger) locally and in Docker Compose, how to reinitialize the database/init scripts, common troubleshooting steps, and useful commands.

## Overview
- Project root for these instructions: `backend/`
- Services in the Compose stack: `postgres`, `redis`, `main-service`, `kitchen-svc`, `jaeger` (Zipkin-compatible endpoint exposed on port `9411`).
- Databases created by init script: `main_app_db`, `kitchen_db`
- DB application user: `myuser` with password: `canti` (default in the repo's init SQL and compose envs)

> Note: We replaced Zipkin with Jaeger (Jaeger exposes a Zipkin-compatible endpoint on 9411) to support arm64 hosts (Apple Silicon / ARM Linux). No code changes were required because services use `spring.zipkin.base-url`.

---

## Prerequisites
- Docker & Docker Compose v2 (or `docker compose` command).
- (Optional for local builds) Java 17 and Maven.
- (Optional) `psql` client for DB checks.

---

## Quick start (recommended)
From the `backend/` folder:

1. Stop and remove any existing stack and volumes (this will remove DB data):
   docker compose down -v --remove-orphans

2. Build and start everything:
   docker compose up -d --build

3. Check status:
   docker compose ps

4. Tail logs:
   docker compose logs --tail=200 --follow main-service kitchen-svc postgres jaeger

5. Open Jaeger UI to view traces: http://localhost:16686  
   Zipkin-compatible endpoint: http://localhost:9411

---

## Reinitialize Postgres / run init scripts
The repository includes `backend/docker/init-db.sql` which is executed only when the Postgres data volume is initialized for the first time. To re-run it (destroy existing DB data):

1. Stop and remove stack and volumes:
   docker compose down -v --remove-orphans

2. Start compose again:
   docker compose up -d --build

3. Verify DB and user exist:
   docker exec -it postgres psql -U postgres -c "\l"
   docker exec -it postgres psql -U myuser -d main_app_db -c "\dt"

---

## Environment variables used by services
Services read these environment variables (they are set in `docker-compose.yml` for container runs; set them in your IDE or shell when running locally):

- SPRING_DATASOURCE_URL (examples)
    - Container: `jdbc:postgresql://postgres:5432/main_app_db`
    - Host (IDE): `jdbc:postgresql://localhost:5432/main_app_db`
- SPRING_DATASOURCE_USERNAME: `myuser`
- SPRING_DATASOURCE_PASSWORD: `canti`
- SPRING_ZIPKIN_BASE_URL: `http://jaeger:9411` (for container runs)
- SPRING_REDIS_HOST / SPRING_REDIS_PORT for redis

You can also override configuration via VM args:
-Dspring.datasource.username=myuser -Dspring.datasource.password=canti

---

## Run services locally (IDE / Maven)
If you prefer to run a service from the IDE:

1. Ensure Postgres is running (via Docker compose or a local Postgres).
2. Set environment variables in the run configuration or export them:
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/main_app_db
   export SPRING_DATASOURCE_USERNAME=myuser
   export SPRING_DATASOURCE_PASSWORD=canti
   export SPRING_ZIPKIN_BASE_URL=http://localhost:9411

3. Start the service (example with Maven):
   mvn -DskipTests -pl main-service -am spring-boot:run

If you want an in-memory DB instead of Postgres for quick dev, use a `local` profile (H2) and run:
SPRING_PROFILES_ACTIVE=local mvn -DskipTests -pl main-service -am spring-boot:run

---

## Building Docker images if Maven downloads fail inside Docker
If the parent Dockerfile runs Maven and dependency downloads fail (common when downloads are truncated), build the jars locally and use a Dockerfile that copies the jar:

1. Build jar locally:
   mvn -DskipTests clean package -pl kitchen-svc -am

2. Build image from the service folder using Dockerfile.dev (or the Dockerfile that copies the prebuilt JAR):
   cd kitchen-svc
   docker build -t kitchen-svc:dev -f Dockerfile.dev --build-arg JAR_FILE=target/<kitchen-jar>.jar .

Then run the container (or update compose to use the local image tag).

---

## Common issues & fixes

1. Database connection refused
    - Symptom: "Connection to localhost:5432 refused"
    - Cause: App tried connecting to `localhost:5432` while running in a container (localhost means the container itself)
    - Fix: Use service hostname in JDBC URL in container runs:
      `jdbc:postgresql://postgres:5432/kitchen_db`

2. Password authentication failed
    - Symptom: "FATAL: password authentication failed for user 'postgres'"
    - Cause: App credentials don't match DB user
    - Fix: Either update service env to use `myuser` / `canti`, or change postgres password:
      docker exec -it postgres psql -U postgres -c "ALTER USER postgres WITH PASSWORD 'canti';"

3. Postgres init script didn't run (databases/users missing)
    - Reason: init scripts are executed only when the data volume is created
    - Fix: Remove the postgres volume and recreate:
      docker compose down -v
      docker compose up -d --build

4. Zipkin image platform mismatch on Apple Silicon (arm64)
    - Symptom: image `openzipkin/zipkin:...` fails due to platform (linux/amd64)
    - Fix we applied: replace Zipkin with `jaegertracing/all-in-one:1.48` which exposes a Zipkin-compatible endpoint on `9411` and runs on arm64. If you need Zipkin specifically, you must find an arm64 tag or enable x86 emulation in your Docker VM.

5. Maven corrupted/partial downloads (dependency truncated)
    - Symptom: `Premature end of Content-Length delimited message body` while downloading a jar
    - Fix:
        - Remove the corrupted cached artifact(s), then force a re-download:
          rm -rf ~/.m2/repository/org/hibernate/orm/hibernate-core/6.6.2.Final
          rm -rf ~/.m2/repository/org/bouncycastle/bcprov-jdk18on/1.80
          mvn -U -DskipTests clean package -pl kitchen-svc -am
        - Alternatively use:
          mvn dependency:purge-local-repository -DmanualInclude=org.hibernate.orm:hibernate-core,org.bouncycastle:bcprov-jdk18on -DreResolve=true

6. Service depends_on references missing service
    - Symptom: `service "main-service" depends on undefined service "zipkin"`
    - Fix: Update `depends_on` for services to reference `jaeger` (or add a `zipkin` service alias using the Jaeger image). Example:
      depends_on:
      postgres:
      condition: service_healthy
      redis:
      condition: service_started
      jaeger:
      condition: service_healthy

---

## Useful commands

- Bring stack down and remove volumes:
  docker compose down -v --remove-orphans

- Build & start (rebuild images):
  docker compose up -d --build

- Build without cache:
  docker compose build --no-cache
  docker compose up -d

- Check container status:
  docker compose ps

- Tail logs:
  docker compose logs --tail=200 --follow main-service kitchen-svc postgres jaeger

- Exec into Postgres and run SQL:
  docker exec -it postgres psql -U postgres -c "\l"
  docker exec -it postgres psql -U myuser -d kitchen_db -c "\dt"

- Remove a broken zipkin container/image:
  docker rm -f zipkin || true
  docker image rm openzipkin/zipkin:2.23.2 || true

- Remove specific corrupted maven cache files (macOS / Linux):
  rm -rf ~/.m2/repository/org/hibernate/orm/hibernate-core/6.6.2.Final
  rm -rf ~/.m2/repository/org/bouncycastle/bcprov-jdk18on/1.80

- Windows PowerShell equivalents:
  Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\org\hibernate\orm\hibernate-core\6.6.2.Final
  Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\org\bouncycastle\bcprov-jdk18on\1.80

---

## Debugging checklist (if a service fails)
1. Run:
   docker compose ps
   docker compose logs --tail=200 <service-name>

2. If DB errors, verify Postgres is healthy:
   docker compose logs --tail=200 postgres
   docker exec -it postgres psql -U postgres -c "\l"

3. If auth issue, check the service environment credentials vs. the DB user.

4. If service starts before Postgres: restart the service after Postgres is healthy:
   docker compose restart <service-name>

5. If problem persists, paste the last ~200 lines of logs for the service and the postgres container here.

---

## Final notes
- For local development where you don't need a persistent DB, use an H2 profile to avoid the need for Postgres.
- Do not commit real production passwords; use secrets management for production.
- If you'd like, I can:
    - add this `HELP.md` into `backend/` (copy/paste for you), or
    - open a PR with the file added, or
    - create a smaller README snippet to include in the top-level project README.

If you want the file committed, tell me and I’ll produce the exact patch text for you to add to the repo.