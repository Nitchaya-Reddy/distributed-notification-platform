.PHONY: up down logs test smoke benchmark clean

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f api worker

test:
	mvn -B verify

smoke:
	./scripts/smoke-test.sh

benchmark:
	python3 scripts/load-test.py --count 200 --concurrency 20

clean:
	docker compose down -v --remove-orphans
