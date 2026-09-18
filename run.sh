#!/usr/bin/env bash
# Starts Kafka (if it isn't already running) and all four CampusGig services, then opens the web UI.
set -e
cd "$(dirname "$0")"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Install it with:  brew install maven   (macOS)"
  echo "or run the services from VS Code instead (see README)."
  exit 1
fi

port_open() { (echo > /dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1; }

wait_for_port() {
  for _ in $(seq 1 90); do
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

mkdir -p logs
for pair in user:8081 job:8082 application:8083 matching:8084; do
  s=${pair%%:*}
  port=${pair##*:}
  if port_open "$port"; then
    echo "==> $s-service is already running on $port, skipping"
    continue
  fi
  echo "==> Starting $s-service"
  mvn -q -pl "$s-service" spring-boot:run > "logs/$s-service.log" 2>&1 &
done

echo "==> Waiting for the services (first run downloads libraries, be patient)"
for p in 8081 8082 8083 8084; do
  wait_for_port $p || { echo "Service on port $p did not start. Check logs/"; exit 1; }
done

echo
echo "CampusGig is running:  http://localhost:8081"
echo "Logs:                  logs/*.log"
echo "Stop everything with:  ./stop.sh"
command -v open >/dev/null 2>&1 && open http://localhost:8081 || true
