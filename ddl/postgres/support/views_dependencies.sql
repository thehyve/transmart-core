CREATE OR REPLACE VIEW public.views_dependencies (
    objtype,
    objschema,
    objname,
    reftype,
    refschema,
    refname ) AS WITH views AS (
    SELECT
        C.oid,
        C.relname AS name,
        N.nspname
    FROM
        pg_class C
    INNER JOIN pg_namespace N ON ( C.relnamespace = N.OID )
    WHERE
        relkind = 'v' )
SELECT DISTINCT
    'v',
    V.nspname,
    V.name,
    C.relkind,
    N.nspname,
    C.relname
FROM
    views V
INNER
    JOIN pg_depend D ON ( D.refobjid = V.oid
        AND D.classid = 2618
        AND D.deptype = 'i' )
    -- find pg_rewrite that depends on view
INNER
    JOIN pg_depend E ON ( E.objid = D.objid
        AND E.refclassid = 1259 )
    -- find pg_class objects on which the pg_rewrite depends
INNER
    JOIN pg_class C ON ( C.oid = E.refobjid )
INNER
    JOIN pg_namespace N ON ( C.relnamespace = N.oid )
WHERE
    V.oid <> C.oid
ORDER BY
	2, 1, 3, 5, 4, 6
