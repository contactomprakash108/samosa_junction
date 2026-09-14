#!/usr/bin/env bash
# Start a user-local PostgreSQL 16 when Docker is not available.
# Superuser/db password: samosa / samosa (same as docker-compose.yml).
#

set -euo pipefail

PGBIN="${HOME}/.local/opt/pgsql-16/bin"
PGLIB="${HOME}/.local/opt/pgsql-16/lib"
PGVAR="${HOME}/.local/var/samosa-pg"
PGDATA="${PGVAR}/data"
export LD_LIBRARY_PATH="${PGLIB}${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"

if [[ ! -x "${PGBIN}/pg_ctl" ]]; then
  echo "PostgreSQL binaries missing at ${PGBIN}."
  echo "They were unpacked from the Zonky Linux amd64 16.8 jar into ~/.local/opt/pgsql-16."
  exit 1
fi

mkdir -p "${PGVAR}"

if [[ ! -f "${PGDATA}/PG_VERSION" ]]; then
  printf '%s\n' 'samosa' > "${PGVAR}/pwfile"
  "${PGBIN}/initdb" -U samosa -A password --pwfile="${PGVAR}/pwfile" -D "${PGDATA}" --encoding=UTF8 --no-locale
  cat >> "${PGDATA}/postgresql.conf" <<EOF
listen_addresses = '127.0.0.1'
port = 5432
unix_socket_directories = '${PGVAR}'
EOF
  printf 'host all all 127.0.0.1/32 scram-sha-256\n' >> "${PGDATA}/pg_hba.conf"
  "${PGBIN}/pg_ctl" -D "${PGDATA}" -l "${PGVAR}/postgres.log" start
  "${PGBIN}/pg_ctl" -D "${PGDATA}" stop
  printf 'CREATE DATABASE samosa_junction OWNER samosa;\n' | "${PGBIN}/postgres" --single -D "${PGDATA}" postgres
fi

if "${PGBIN}/pg_ctl" -D "${PGDATA}" status >/dev/null 2>&1; then
  echo "PostgreSQL already running on 127.0.0.1:5432"
else
  "${PGBIN}/pg_ctl" -D "${PGDATA}" -l "${PGVAR}/postgres.log" start
  echo "PostgreSQL started on 127.0.0.1:5432 (db samosa_junction)"
fi
