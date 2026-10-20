#!/usr/bin/env python3
"""Dependency-free local benchmark for the notification platform.

Example:
  python3 scripts/load-test.py --count 200 --concurrency 20

The benchmark intentionally separates two measurements:
1. HTTP request-acceptance latency/throughput measured by the client.
2. Notification lifecycle latency measured from the server timestamps
   (createdAt -> terminal updatedAt), so polling cadence does not inflate E2E latency.

Terminal statuses are polled in rounds with bounded concurrency. Each accepted
notification gets its own timeout deadline from the instant the POST response is
received; notifications never wait in a queue before their timeout begins.

Do not treat local-machine measurements as universal production capacity.
"""
from __future__ import annotations

import argparse
import json
import statistics
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from urllib import error, request

TERMINAL_STATUSES = {"SENT", "FAILED"}


def percentile(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    idx = min(len(ordered) - 1, max(0, int(round((p / 100) * (len(ordered) - 1)))))
    return ordered[idx]


def parse_instant(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def submit(base: str, i: int) -> dict:
    payload = json.dumps({
        "recipient": f"load-{i}-{uuid.uuid4().hex[:8]}@example.com",
        "subject": "Load test",
        "body": "Benchmark notification",
        "channels": ["EMAIL"],
    }).encode()
    req = request.Request(
        f"{base}/api/v1/notifications",
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": f"load-{uuid.uuid4()}",
        },
    )

    started = time.perf_counter()
    try:
        with request.urlopen(req, timeout=10) as resp:
            body = json.loads(resp.read())
            return {
                "id": body.get("id"),
                "http_status": resp.status,
                "acceptance_seconds": time.perf_counter() - started,
                "accepted_perf": time.perf_counter(),
                "created_at": body.get("createdAt"),
            }
    except error.HTTPError as exc:
        return {
            "id": None,
            "http_status": exc.code,
            "acceptance_seconds": time.perf_counter() - started,
            "accepted_perf": time.perf_counter(),
            "created_at": None,
        }
    except Exception as exc:  # network/timeout errors are benchmark results too
        return {
            "id": None,
            "http_status": f"ERROR:{type(exc).__name__}",
            "acceptance_seconds": time.perf_counter() - started,
            "accepted_perf": time.perf_counter(),
            "created_at": None,
        }


def fetch_status(base: str, notification_id: str) -> dict | None:
    try:
        with request.urlopen(f"{base}/api/v1/notifications/{notification_id}", timeout=5) as resp:
            return json.loads(resp.read())
    except Exception:
        return None


def monitor_terminals(
    base: str,
    accepted: list[dict],
    *,
    poll_concurrency: int,
    poll_interval: float,
    terminal_timeout: float,
) -> tuple[dict[str, int], list[float], list[tuple[datetime, datetime]], int]:
    """Poll all accepted notifications fairly until terminal or per-item timeout.

    E2E latency comes from server timestamps rather than poll-observation time.
    This prevents a slow polling queue or a coarse poll interval from inflating
    the reported notification lifecycle latency.
    """
    pending: dict[str, dict] = {}
    for item in accepted:
        pending[item["id"]] = {
            **item,
            "deadline": item["accepted_perf"] + terminal_timeout,
        }

    terminal_counts: dict[str, int] = {}
    e2e_seconds: list[float] = []
    lifecycle_windows: list[tuple[datetime, datetime]] = []
    poll_rounds = 0

    with ThreadPoolExecutor(max_workers=poll_concurrency) as pool:
        while pending:
            poll_rounds += 1
            round_ids = list(pending.keys())
            futures = {
                pool.submit(fetch_status, base, notification_id): notification_id
                for notification_id in round_ids
            }

            for future in as_completed(futures):
                notification_id = futures[future]
                if notification_id not in pending:
                    continue

                body = future.result()
                if not body:
                    continue

                status = body.get("status")
                if status not in TERMINAL_STATUSES:
                    continue

                item = pending.pop(notification_id)
                terminal_counts[status] = terminal_counts.get(status, 0) + 1

                created_at = parse_instant(body.get("createdAt") or item.get("created_at"))
                updated_at = parse_instant(body.get("updatedAt"))
                if created_at and updated_at and updated_at >= created_at:
                    e2e_seconds.append((updated_at - created_at).total_seconds())
                    lifecycle_windows.append((created_at, updated_at))

            now = time.perf_counter()
            expired = [
                notification_id
                for notification_id, item in pending.items()
                if now >= item["deadline"]
            ]
            for notification_id in expired:
                pending.pop(notification_id, None)
                terminal_counts["TIMEOUT"] = terminal_counts.get("TIMEOUT", 0) + 1

            if pending:
                time.sleep(poll_interval)

    return terminal_counts, e2e_seconds, lifecycle_windows, poll_rounds


def print_latency(label: str, values_seconds: list[float]) -> None:
    if not values_seconds:
        return
    print(
        f"{label}: mean={statistics.mean(values_seconds) * 1000:.2f} "
        f"p50={percentile(values_seconds, 50) * 1000:.2f} "
        f"p95={percentile(values_seconds, 95) * 1000:.2f} "
        f"max={max(values_seconds) * 1000:.2f}"
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default="http://localhost:8080")
    ap.add_argument("--count", type=int, default=100)
    ap.add_argument("--concurrency", type=int, default=10)
    ap.add_argument("--poll-concurrency", type=int, default=None)
    ap.add_argument("--poll-interval", type=float, default=2.0)
    ap.add_argument("--terminal-timeout", type=float, default=60.0)
    args = ap.parse_args()

    if args.count <= 0 or args.concurrency <= 0:
        ap.error("--count and --concurrency must be positive")
    if args.poll_interval <= 0 or args.terminal_timeout <= 0:
        ap.error("--poll-interval and --terminal-timeout must be positive")

    poll_concurrency = args.poll_concurrency or min(args.concurrency, 20)
    poll_concurrency = max(1, poll_concurrency)

    acceptance: list[float] = []
    accepted: list[dict] = []
    http_statuses: dict[object, int] = {}

    total_start = time.perf_counter()
    submit_start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = [pool.submit(submit, args.base_url, i) for i in range(args.count)]
        for future in as_completed(futures):
            result = future.result()
            status = result["http_status"]
            http_statuses[status] = http_statuses.get(status, 0) + 1
            acceptance.append(result["acceptance_seconds"])
            if result["id"] and status == 202:
                accepted.append(result)
    submission_seconds = time.perf_counter() - submit_start

    monitor_start = time.perf_counter()
    terminal, e2e, lifecycle_windows, poll_rounds = monitor_terminals(
        args.base_url,
        accepted,
        poll_concurrency=poll_concurrency,
        poll_interval=args.poll_interval,
        terminal_timeout=args.terminal_timeout,
    )
    monitor_seconds = time.perf_counter() - monitor_start
    total_seconds = time.perf_counter() - total_start

    accepted_count = len(accepted)
    acceptance_rps = accepted_count / submission_seconds if submission_seconds > 0 else 0.0

    terminal_count = sum(terminal.get(status, 0) for status in TERMINAL_STATUSES)
    terminal_success_rate = (terminal_count / accepted_count * 100) if accepted_count else 0.0

    completion_rate = 0.0
    lifecycle_window_seconds = 0.0
    if lifecycle_windows:
        first_created = min(pair[0] for pair in lifecycle_windows)
        last_updated = max(pair[1] for pair in lifecycle_windows)
        lifecycle_window_seconds = max(0.0, (last_updated - first_created).total_seconds())
        if lifecycle_window_seconds > 0:
            completion_rate = len(lifecycle_windows) / lifecycle_window_seconds

    print(
        f"requests={args.count} concurrency={args.concurrency} "
        f"poll_concurrency={poll_concurrency} poll_interval_seconds={args.poll_interval:.2f}"
    )
    print(
        f"submission_wall_seconds={submission_seconds:.3f} "
        f"monitor_wall_seconds={monitor_seconds:.3f} total_wall_seconds={total_seconds:.3f}"
    )
    print(f"http_statuses={http_statuses}")
    print(f"terminal_statuses={terminal}")
    print(f"accepted_requests_per_second={acceptance_rps:.2f}")
    print(f"observed_terminal_rate_percent={terminal_success_rate:.2f}")
    print(f"poll_rounds={poll_rounds}")
    if lifecycle_windows:
        print(
            f"server_lifecycle_window_seconds={lifecycle_window_seconds:.3f} "
            f"terminal_completions_per_second={completion_rate:.2f}"
        )

    print_latency("acceptance_ms", acceptance)
    print_latency("server_end_to_end_ms", e2e)
    print(
        "Method: HTTP acceptance is client-timed; end-to-end latency uses server "
        "createdAt->updatedAt timestamps, so polling cadence does not inflate it."
    )
    print("Use these numbers only with the exact test environment and workload documented.")


if __name__ == "__main__":
    main()
