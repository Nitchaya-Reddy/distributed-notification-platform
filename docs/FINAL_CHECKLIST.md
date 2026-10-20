# Pre-publish checklist

Use this once after the final code changes and before publishing the repository.

## 1. Start the stack

```bash
docker compose down
docker compose up --build -d
```

## 2. Verify health

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8081/actuator/health
```

Both should report `UP`.

## 3. Run the terminal-state smoke test

```bash
./scripts/smoke-test.sh
```

Expected final result: `Smoke test passed.` and notification status `SENT`.

## 4. Run the corrected benchmark

```bash
python3 scripts/load-test.py --count 200 --concurrency 20
```

Save the complete output. Resume metrics must come from this corrected benchmark, not the legacy polling implementation.

## 5. Confirm Kafka lag drained

```bash
docker compose exec kafka \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:19092 \
  --group notification-delivery-workers \
  --describe
```

```bash
docker compose exec kafka \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:19092 \
  --group notification-dispatcher \
  --describe
```

After the benchmark finishes, all partitions should eventually return to `LAG = 0`.

## 6. Verify delivery status in PostgreSQL

```bash
docker compose exec postgres psql -U notify -d notifications -c \
"SELECT status, COUNT(*) FROM notifications GROUP BY status ORDER BY status;"
```

## 7. Check repository hygiene

```bash
git status
find . -name '.DS_Store' -o -name '*.log'
```

Never commit `.env`, credentials, provider API keys, IDE state, logs, or build output.

## 8. Publish

```bash
git init
git branch -M main
git add .
git commit -m "Build distributed notification platform"
git remote add origin https://github.com/YOUR_USERNAME/distributed-notification-platform.git
git push -u origin main
```

Then wait for the GitHub Actions `CI` workflow to pass before linking the repository from a resume.
