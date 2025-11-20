#!/usr/bin/env bash
set -euo pipefail

# Configurable via environment variables
PGHOST=${PGHOST:-postgres}
PGPORT=${PGPORT:-5432}
PGSUPERUSER=${PGSUPERUSER:-postgres}
PGSUPERPASS=${PGSUPERPASS:-canti}
APP_USER=${APP_USER:-myuser}
APP_PASS=${APP_PASS:-canti}
DBS=${DBS:-main_app_db kitchen_db}
RETRIES=${RETRIES:-60}
SLEEP=${SLEEP:-1}

export PGPASSWORD="$PGSUPERPASS"

echo "Waiting for Postgres at $PGHOST:$PGPORT..."

i=0
while ! psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -d postgres -c '\l' >/dev/null 2>&1; do
  i=$((i+1))
  if [ "$i" -ge "$RETRIES" ]; then
    echo "Postgres did not become available after $RETRIES attempts" >&2
    exit 1
  fi
  sleep "$SLEEP"
done

echo "Postgres is available, ensuring role and databases..."

# Ensure role (user) exists
role_exists=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -tAc "SELECT 1 FROM pg_roles WHERE rolname='${APP_USER}'")
if [ "$role_exists" = "1" ]; then
  echo "Role ${APP_USER} already exists"
else
  echo "Creating role ${APP_USER}"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -c "CREATE ROLE \"${APP_USER}\" WITH LOGIN PASSWORD '${APP_PASS}';"
fi

# Create each database if missing and set owner
for db in $DBS; do
  db_exists=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -tAc "SELECT 1 FROM pg_database WHERE datname='${db}'")
  if [ "$db_exists" = "1" ]; then
    echo "Database ${db} already exists"
  else
    echo "Creating database ${db} owned by ${APP_USER}"
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -c "CREATE DATABASE \"${db}\" OWNER \"${APP_USER}\";"
  fi
done

echo "Ensuring privileges on databases..."
for db in $DBS; do
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -c "GRANT ALL PRIVILEGES ON DATABASE \"${db}\" TO \"${APP_USER}\";"
done

echo "Done."
exit 0