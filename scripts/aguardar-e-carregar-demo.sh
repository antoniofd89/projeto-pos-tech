#!/bin/sh
set -eu

tentativas=0
limite=60

echo "Aguardando PostgreSQL e migrations da aplicação..."
while [ "$tentativas" -lt "$limite" ]; do
    if pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" >/dev/null 2>&1 \
        && [ "$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -tAc "SELECT to_regclass('public.ordens_servico') IS NOT NULL" 2>/dev/null || true)" = "t" ]; then
        echo "Banco e schema prontos; aplicando carga de demonstração."
        exec psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -f /scripts/carga-demonstracao.sql
    fi
    tentativas=$((tentativas + 1))
    sleep 2
done

echo "Erro: PostgreSQL/schema não ficou pronto em 120 segundos; carga de demonstração não aplicada." >&2
exit 1
