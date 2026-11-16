#!/usr/bin/env bash
# Runs the JMeter load test against the stack started with `docker compose up -d`
# and records memory and CPU of the backend container every 2 seconds.
set -euo pipefail
cd "$(dirname "$0")"

DURATION="${DURATION:-60}"
OBSERVERS="${OBSERVERS:-20}"
UNITS="${UNITS:-100}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-wargame-backend-1}"
JMETER_IMAGE="${JMETER_IMAGE:-alpine/jmeter:5.6.3}"
RESULTS="results/$(date +%Y%m%d-%H%M%S)"

mkdir -p "$RESULTS"
echo "time,memory_usage,memory_percent,cpu_percent" > "$RESULTS/backend-resources.csv"

monitor_backend() {
    while true; do
        docker stats --no-stream --format '{{.MemUsage}},{{.MemPerc}},{{.CPUPerc}}' "$BACKEND_CONTAINER" \
            | sed "s/^/$(date +%H:%M:%S),/" >> "$RESULTS/backend-resources.csv"
        sleep 2
    done
}

monitor_backend &
MONITOR_PID=$!
trap 'kill "$MONITOR_PID" 2>/dev/null || true' EXIT

docker run --rm --network host --user "$(id -u):$(id -g)" \
    -v "$PWD:/load-tests" -w /load-tests "$JMETER_IMAGE" \
    -n -t broker-load.jmx \
    -Jduration="$DURATION" -Jobservers="$OBSERVERS" -Junits="$UNITS" \
    -j "$RESULTS/jmeter.log" -l "$RESULTS/results.jtl" -e -o "$RESULTS/report"

echo "Results: load-tests/$RESULTS (HTML report: report/index.html, resources: backend-resources.csv)"
