\set ON_ERROR_STOP on
-- Support definitions for the assign_* scripts
DO $$
<<locals>>
DECLARE
    dummy        record;
BEGIN
    -- Make sure we have array_accum aggregate
    SELECT proname
    INTO dummy
    FROM
        pg_proc p
        JOIN pg_namespace n ON (p.pronamespace = n.oid)
    WHERE
        n.nspname = 'public'
        AND proname = 'array_accum'
        AND proargtypes = ARRAY[2283]::oidvector; --oid for anyelement

    IF NOT FOUND THEN
        CREATE AGGREGATE public.array_accum(anyelement) (
            sfunc = array_append,
            stype = anyarray,
            initcond = '{}'
        );
    END IF;

    -- Ensure biomart_write_tables exists
    SELECT relname
    INTO dummy
    FROM pg_class c
    WHERE relname = 'biomart_write_tables';

    IF NOT FOUND THEN
        CREATE TABLE public.biomart_write_tables(
            nschema text,
            ntable text);
    END IF;

    -- Ensure ts_default_permissions exists
    SELECT relname
    INTO dummy
    FROM pg_class c
    WHERE relname = 'ts_default_permissions';

    IF NOT FOUND THEN
        CREATE TABLE public.ts_default_permissions(
            nschema text,
            nuser text,
            ntype text,
            nperm text);
    END IF;

    -- Make sure we have the schemas_tables_funcs view
    CREATE OR REPLACE VIEW public.schemas_tables_funcs(
        name,
        kind,
        owner,
        acl,
        nspname
    ) AS
    (  -- tables, views and sequences
        SELECT
            quote_ident(relname),
            relkind,
            rolname,
            relacl,
            nspname
        FROM
            pg_class c
            JOIN pg_namespace n ON (c.relnamespace = n.oid)
            JOIN pg_roles r ON (c.relowner = r.oid)
        WHERE
            c.relkind IN ('r','S','v')
        ORDER BY c.relkind = 'S'
    )
    UNION
    ( -- schemas
        SELECT
            nspname,
            's',
            rolname,
            nspacl,
            nspname
        FROM
            pg_namespace n
            JOIN pg_roles r ON (n.nspowner = r.oid)
    )
    UNION
    ( -- functions (including aggregates)
        SELECT
            quote_ident(p.proname) || '(' || array_to_string((
                    SELECT
                        public.array_accum (typname)
                    FROM
                        UNNEST(p.proargtypes) AS A(oid)
                        JOIN pg_type T ON (T.oid = A.oid)),
                ', ') || ')',
            'f',
            rolname,
            proacl,
            nspname
        FROM
            pg_proc p
            JOIN pg_namespace n ON (p.pronamespace = n.oid)
            JOIN pg_roles r ON (p.proowner = r.oid)
    );
END;
$$ LANGUAGE plpgsql;

TRUNCATE public.biomart_write_tables;

\COPY public.biomart_write_tables FROM 'biomart_user_write.tsv'

TRUNCATE public.ts_default_permissions;

\COPY public.ts_default_permissions FROM 'default_permissions.tsv'

-- vim: ft=plsql ts=4 sw=4 et:
