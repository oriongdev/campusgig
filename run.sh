#!/usr/bin/env bash
# Starts Kafka (if it isn't already running) and all four CampusGig services, then opens the web UI.
#
#   ./run.sh             real-time analytics from in-memory counters (default)
#   ./run.sh --streams   real-time analytics from Kafka Streams
set -e
cd "$(dirname "$0")"

MATCHING_PROFILE=""
case "${1:-}" in
  "") ;;
  --streams) MATCHING_PROFILE="streams" ;;
  *) echo "Usage: ./run.sh [--streams]"; exit 1 ;;
esac

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Install it with:  brew install maven   (macOS)"
  echo "or run the services from VS Code instead (see README)."
  exit 1
fi

port_open() { (echo > /dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1; }

wait_for_port() {
  for _ in $(seq 1 "${2:-90}"); do
    port_open "$1" && return 0
    sleep 1
  done
  return 1
}

if port_open 9092; then
  echo "==> Kafka is already running on 9092, using it"
else
  echo "==> Starting Kafka"
  if command -v brew >/dev/null 2>&1 && brew list kafka >/dev/null 2>&1; then
    brew services start kafka
  elif docker info >/dev/null 2>&1; then
    docker compose up -d
  else
    echo "No Kafka found. Install it with 'brew install kafka', or start Docker Desktop."
    exit 1
  fi
  echo "==> Waiting for Kafka on 9092"
  wait_for_port 9092 || { echo "Kafka did not start."; exit 1; }
fi

if [ -z "${GEMINI_API_KEY:-}" ]; then
  echo "==> GEMINI_API_KEY is not set, so the two AI buttons will be greyed out."
  echo "    To use the AI agents: stop, run  export GEMINI_API_KEY=your-key  and start again."
fi

mkdir -p logs
for pair in user:8081 job:8082 application:8083 matching:8084; do
  s=${pair%%:*}
  port=${pair##*:}
  if port_open "$port"; then
    echo "==> $s-service is already running on $port, skipping"
    continue
  fi
  if [ "$s" = matching ] && [ -n "$MATCHING_PROFILE" ]; then
    echo "==> Starting $s-service with Kafka Streams analytics"
    SPRING_PROFILES_ACTIVE="$MATCHING_PROFILE" mvn -q -pl "$s-service" spring-boot:run > "logs/$s-service.log" 2>&1 &
  else
    echo "==> Starting $s-service"
    mvn -q -pl "$s-service" spring-boot:run > "logs/$s-service.log" 2>&1 &
  fi
done

echo "==> Waiting for the services (the first run downloads libraries, so give it a few minutes)"
for p in 8081 8082 8083 8084; do
  wait_for_port $p 300 || { echo "The service on port $p did not start. Look at the end of its file in logs/"; exit 1; }
done

echo
echo "CampusGig is running:  http://localhost:8081"
echo "Logs:                  logs/*.log"
echo "Try every feature:     ./demo.sh"
echo "Stop everything with:  ./stop.sh"
if [ "$(uname)" = Darwin ]; then
  open http://localhost:8081 || true
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open http://localhost:8081 >/dev/null 2>&1 || true
fi
