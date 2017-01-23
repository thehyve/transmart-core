#!/bin/bash -e

set -x

{ read -r -d '' QUERY || true; } <<'EOD'
select C2.relname
FROM pg_index I
INNER JOIN pg_class C ON (C.oid = I.indrelid)
INNER JOIN pg_class C2 ON (C2.oid = I.indexrelid AND C2.relkind = 'i')
INNER JOIN pg_namespace N ON (C.relnamespace = N.oid)
WHERE nspname = 'deapp' AND C.relname = 'de_rc_snp_info';
EOD

PIDS=""

echo "This will take a while..."
for idx in $($PSQL_COMMAND --tuples-only --no-align -c "$QUERY"); do
  echo "Rebuild $idx"
  $PSQL_COMMAND -c "REINDEX INDEX deapp.$idx" &
  PIDS="$PIDS $!"
done

for pid in $PIDS; do wait $pid; done

echo "Done"
