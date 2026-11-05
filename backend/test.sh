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
    # pg_isready risponde anche durante l'inizializzazione, prima che il database esista.
    if docker exec "$NAME" psql -U postgres -d gymlog -c 'select 1' >/dev/null 2>&1; then
        ready=1
        break
    fi
    sleep 1
done
if [ "${ready:-}" != "1" ]; then
    echo "backend: Postgres non pronto" >&2
    exit 1
fi

run() {
    docker exec -i "$NAME" psql -v ON_ERROR_STOP=1 -q -U postgres -d gymlog -f -
}

run < supabase/tests/00_stub.sql
run < supabase/migrations/20261104120000_catalog.sql
run < supabase/migrations/20261104130000_tombstones.sql
run < supabase/migrations/20261104140000_image_editors_realtime.sql
run < supabase/tests/10_catalog.sql

echo "backend: ok"
