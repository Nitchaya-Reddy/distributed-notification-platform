#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
COUNT="${COUNT:-100}"
CONCURRENCY="${CONCURRENCY:-10}"
export BASE_URL

seq 1 "$COUNT" | xargs -I{} -P "$CONCURRENCY" bash -c '
  curl -sS -o /dev/null -w "%{http_code}\n" -X POST "$BASE_URL/api/v1/notifications" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: load-{}-$(date +%s%N)" \
    -d "{\"recipient\":\"load{}@example.com\",\"body\":\"load test\",\"channels\":[\"EMAIL\"]}"
' | sort | uniq -c

echo "Submitted $COUNT requests with concurrency=$CONCURRENCY. Capture real latency/throughput before adding metrics to a resume."
