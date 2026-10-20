#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-30}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-1}"

response=$(curl -fsS -X POST "$BASE_URL/api/v1/notifications" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: smoke-$(date +%s)-$RANDOM" \
  -d '{"recipient":"smoke@example.com","subject":"Smoke test","body":"hello","channels":["EMAIL","PUSH"]}')

echo "Created notification:"
echo "$response"

id=$(printf '%s' "$response" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id", ""))')
if [[ -z "$id" ]]; then
  echo "Could not parse notification id" >&2
  exit 1
fi

echo "Waiting for $id to reach SENT or FAILED..."
start=$SECONDS
attempt=0

while (( SECONDS - start < TIMEOUT_SECONDS )); do
  attempt=$((attempt + 1))
  result=$(curl -fsS "$BASE_URL/api/v1/notifications/$id")
  status=$(printf '%s' "$result" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", "UNKNOWN"))')
  echo "Attempt $attempt: status=$status"

  if [[ "$status" == "SENT" ]]; then
    echo "$result"
    echo "Smoke test passed."
    exit 0
  fi

  if [[ "$status" == "FAILED" ]]; then
    echo "$result" >&2
    echo "Smoke test failed: notification reached FAILED." >&2
    exit 1
  fi

  sleep "$POLL_INTERVAL_SECONDS"
done

echo "Smoke test timed out after ${TIMEOUT_SECONDS}s." >&2
exit 1
