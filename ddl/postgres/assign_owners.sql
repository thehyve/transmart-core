DO $$
<<locals>>
DECLARE
    cur_owner    text;
    wanted_owner text;
    command      text;
    spec         text[];
    schema_name  text;
    obj_name     text;
    obj_type     char;
    exceptions   text[][];
    dummy        record;
BEGIN
    RAISE NOTICE 'Started assigning owners';

    -- One must be owner to create child tables
    exceptions := ARRAY[
        -- schema, object, owner
        ['biomart', 'heat_map_results', 'tm_cz']
    ];

    -- Make sure we have array_accum aggregate
    SELECT proname
    INTO dummy
    FROM
        pg_proc p
        JOIN pg_namespace n ON ( p.pronamespace = n.oid )
    WHERE
        n.nspname = 'public'
        AND proname = 'array_accum'
        AND proargtypes = ARRAY [ 2283 ] ::oidvector;

    IF NOT FOUND THEN
        CREATE AGGREGATE public.array_accum(anyelement) (
            sfunc = array_append,
            stype = anyarray,
            initcond = '{}'
        );
    END IF;

    -- Convert array to table
    CREATE TEMPORARY TABLE ownership_exceptions(
        schema_name,
        object_name,
        proper_owner)
        AS SELECT exceptions[idx][1], exceptions[idx][2], exceptions[idx][3]
        FROM generate_series(1, array_upper(exceptions, 1)) as b(idx);

    spec := ARRAY[
        'tm_cz',
        'tm_lz',
        'tm_wz',
        'i2b2demodata',
        'i2b2metadata',
        'deapp',
        'searchapp',
        'biomart'
    ];

    FOREACH schema_name IN ARRAY spec LOOP

        -- http://stackoverflow.com/a/6852484/127724
        FOR obj_name, obj_type, cur_owner IN
                (
                    SELECT
                        quote_ident(relname) AS relname,
                        relkind,
                        rolname
                    FROM
                        pg_class c
                        JOIN pg_namespace n ON (c.relnamespace = n.oid)
                        JOIN pg_roles r ON (c.relowner = r.oid)
                    WHERE
                        n.nspname = schema_name
                        AND c.relkind IN ('r','S','v')
                    ORDER BY c.relkind = 'S'
                )
                UNION
                (
                    SELECT
                        quote_ident(p.proname) || '(' || array_to_string((
                                SELECT
                                    public.array_accum (typname)
                                FROM
                                    UNNEST(p.proargtypes) AS A(oid)
                                    JOIN pg_type T ON (T.oid = A.oid)),
                            ', ') || ')',
                        'f',
                        rolname
                    FROM
                        pg_proc p
                        JOIN pg_namespace n ON ( p.pronamespace = n.oid )
                        JOIN pg_roles r ON ( p.proowner = r.oid )
                    WHERE
                        n.nspname = schema_name
                ) LOOP

            SELECT proper_owner
            INTO wanted_owner
            FROM ownership_exceptions E
            WHERE E.schema_name = locals.schema_name
                AND E.object_name = locals.obj_name;

            IF NOT FOUND THEN
                wanted_owner := schema_name;
            END IF;

            IF cur_owner = wanted_owner THEN
                CONTINUE;
            END IF;

            RAISE NOTICE 'The owner of % %.% is %; changing to %',
                    CASE obj_type
                        WHEN 'r' THEN 'table'
                        WHEN 'S' THEN 'sequence'
                        WHEN 'v' THEN 'view'
                        WHEN 'f' THEN 'function'
                    END,
                    schema_name,
                    obj_name,
                    cur_owner,
                    wanted_owner;

            -- ALTER TABLE can be used for all the types here
            command := 'ALTER ' ||
                    CASE obj_type
                        WHEN 'f' THEN 'FUNCTION'
                        ELSE 'TABLE'
                    END || ' ' || schema_name || '.' || obj_name
                    || ' OWNER TO ' || wanted_owner;

            EXECUTE(command);

        END LOOP;
    END LOOP;

    RAISE NOTICE 'Finished assigning owners';

END;
$$ LANGUAGE plpgsql;

-- vim: ft=plsql ts=4 sw=4 et:
