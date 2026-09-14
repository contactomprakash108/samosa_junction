#!/usr/bin/env bash
# Start Redis on 127.0.0.1:6379 when Docker is not available.
#

set -euo pipefail

if command -v redis-cli >/dev/null 2>&1 && redis-cli ping >/dev/null 2>&1; then
  echo "Redis already running on 127.0.0.1:6379"
  exit 0
fi

if command -v redis-server >/dev/null 2>&1; then
  redis-server --daemonize yes --bind 127.0.0.1 --port 6379 --save "" --appendonly no
  echo "Started redis-server on 127.0.0.1:6379"
  exit 0
fi

cat <<'EOF'
Redis is not installed in this environment.

Install one of:
  sudo apt install redis-server
  docker compose up -d redis

The API still runs without Redis: the cart falls back to Postgres JSONB.
EOF
exit 1
