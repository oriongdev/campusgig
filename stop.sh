#!/usr/bin/env bash
# Stops the four services and Kafka.
cd "$(dirname "$0")"

pids=$(lsof -ti tcp:8081-8084 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$pids" ]; then
  echo "==> Stopping services"
  kill $pids 2>/dev/null || true
fi

if docker info >/dev/null 2>&1 && docker compose ps -q kafka >/dev/null 2>&1; then
  echo "==> Stopping Kafka (Docker)"
  docker compose down
elif command -v brew >/dev/null 2>&1; then
  echo "==> Stopping Kafka (Homebrew)"
  brew services stop kafka
fi
echo "Done."
