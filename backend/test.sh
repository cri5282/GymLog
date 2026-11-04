#!/usr/bin/env bash
# Verifica le migrazioni SQL su un Postgres vero, con stub di auth/storage.
# Non tocca nessun progetto Supabase e non lascia niente acceso: container --rm.
set -euo pipefail
cd "$(dirname "$0")"

NAME="gymlog-backend-test-$$"
IMAGE="postgres:16-alpine"

cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

docker run -d --rm --name "$NAME" \
    -e POSTGRES_PASSWORD=test -e POSTGRES_DB=gymlog "$IMAGE" >/dev/null

for _ in $(seq 1 60); do
    if docker exec "$NAME" pg_isready -U postgres -d gymlog >/dev/null 2>&1; then break; fi
    sleep 1
done

run() {
    docker exec -i "$NAME" psql -v ON_ERROR_STOP=1 -q -U postgres -d gymlog -f -
}

run < supabase/tests/00_stub.sql
run < supabase/migrations/20261104120000_catalog.sql
run < supabase/tests/10_catalog.sql

echo "backend: ok"
