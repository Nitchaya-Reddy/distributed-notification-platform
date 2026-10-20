# Benchmarking methodology

The repository includes a dependency-free local benchmark:

```bash
python3 scripts/load-test.py --count 200 --concurrency 20
```

## What is measured

The benchmark separates API acceptance from asynchronous delivery:

- **Acceptance latency** is measured on the client around the HTTP `POST` request.
- **Accepted requests/second** is calculated only over the request-submission phase.
- **End-to-end notification latency** uses the API's server-side `createdAt` and terminal `updatedAt` timestamps. This prevents status-poll scheduling from inflating lifecycle latency.
- **Terminal completion rate** is computed over the server lifecycle window from the earliest accepted notification to the latest terminal update.

Status polling is deliberately bounded and coarse by default (`2s`) because it is an observation mechanism, not the workload being benchmarked. Since end-to-end latency uses server timestamps, increasing the poll interval does not add artificial E2E latency.

## Useful options

```bash
python3 scripts/load-test.py \
  --count 200 \
  --concurrency 20 \
  --poll-concurrency 20 \
  --poll-interval 2 \
  --terminal-timeout 60
```

## Before quoting a number

Record all of the following with the result:

- request count and client concurrency
- Docker/runtime environment and machine class
- provider mode (the repository defaults to simulated/logging adapters)
- Kafka partition counts and listener concurrency
- whether the stack was warm or freshly restarted
- exact commit SHA

Do not describe local simulated-provider results as production throughput or vendor-delivery latency.

## Kafka concurrency used by the portfolio configuration

- `notifications.requested`: 6 partitions, dispatcher concurrency `3`
- `notifications.delivery`: 12 partitions, delivery-worker concurrency `6`
- `notifications.status`: 3 partitions, status-consumer concurrency `3`

Kafka preserves ordering within a partition. The application keys events by notification identifier where ordering matters, allowing different notifications to be processed in parallel while preserving per-key ordering.
