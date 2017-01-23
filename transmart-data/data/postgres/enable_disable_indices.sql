update pg_index I
SET indisready = %ENABLED%
FROM pg_class C
INNER JOIN pg_namespace N ON (C.relnamespace = N.oid)
WHERE C.oid = I.indrelid AND nspname = '%SCHEMA%' AND C.relname = '%TABLE%';
