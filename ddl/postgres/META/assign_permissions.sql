\set ON_ERROR_STOP on
-- The strategy for permissions will be:
-- – Assign all permissions to the owner.
-- – If a table owner does not match the schema name, assign all permissions
--   the role with the same name as the schema.
-- – Assign all permissions on all objects to tm_cz, which even needs to
--   drop and create tables in some cases.
-- — Assign read/usage permissions on all tables, views, sequences and
--   functions to biomart_user on all non-ETL schemas.
-- – Assign insert,delete,update permissions on all tables on searchapp.
-- – Assign insert,delete,update permissions on a manually defined set of
--   tables to biomart_user, as required by the tranSMART application.

-- If the table owner does not match the schema owner, assign all permissions to
-- the schema owner.
CREATE OR REPLACE FUNCTION public.assign_permissions_1(name text, schema_name text, type char, owner text)
        RETURNS aclitem AS $$
BEGIN
    IF schema_name = owner OR type <> 'r' /* table */ THEN
        RETURN NULL;
    END IF;

    RETURN 'user ' || schema_name || '=arwdDxt/' || (SELECT current_user);
END;
$$ LANGUAGE plpgsql;

-- Assign all permissions on all objects to tm_cz, which even needs to
-- drop and create tables in some cases.
-- ALSO assign all permissions to the owner
CREATE OR REPLACE FUNCTION public.assign_permissions_2(name text, schema_name text, type char, owner text)
        RETURNS aclitem[] AS $$
DECLARE
    allPermissions text;
    result         aclitem[];
BEGIN
    allPermissions :=
        CASE type
            WHEN 'r' THEN 'arwdDxt'
            WHEN 'S' THEN 'rwU'
            WHEN 'v' THEN 'arwdDxt'
            WHEN 'f' THEN 'X'
            WHEN 's' THEN 'UC'
        END ||
        '/' || (SELECT current_user);

    result := ARRAY[
        ('user ' || owner || '=' || allPermissions)::aclitem
    ];
    IF owner <> 'tm_cz' THEN
        result := result || ('user tm_cz=' || allPermissions)::aclitem;
    END IF;

    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Assign read/usage permissions on all tables, views, sequences and
-- functions to biomart_user, except if:
-- 1) it's an ETL schema, in which case no permissions should be granted
-- 2) the schema is searchapp, in which case deletes, inserts and updates
--    should be allowed on all tables
-- 3) it's one of the exception tables for which biomart_user should be granted
--    insert, update and delete permissions.
CREATE OR REPLACE FUNCTION public.assign_permissions_3(name text, schema_name text, type char, owner text)
        RETURNS aclitem AS $$
DECLARE
    dummy record;
BEGIN
    IF NOT schema_name = ANY(ARRAY['i2b2demodata', 'i2b2metadata', 'deapp',
            'searchapp', 'biomart']) THEN
        RETURN NULL;
    END IF;

    IF schema_name = 'searchapp' AND type = 'r' /* table */ THEN
        RETURN 'user biomart_user=arwdDxt/' || (SELECT current_user);
    END IF;

    IF type = 'r' /* table */ THEN
        SELECT *
        INTO dummy
        FROM public.biomart_write_tables B
        WHERE B.nschema = schema_name
            AND B.ntable = name;

        IF FOUND THEN
            -- append (insert), read (select), write (update), delete
            RETURN 'user biomart_user=arwd/' || (SELECT current_user);
        END IF;
    END IF;

    RETURN 'user biomart_user=' ||
            CASE type
                WHEN 'r' THEN 'r'
                WHEN 'S' THEN 'rwU' -- from the docs, w or U would suffice
                WHEN 'v' THEN 'r'
                WHEN 'f' THEN 'X'
                WHEN 's' THEN 'U' -- usage only; not create
            END ||
        '/' || (SELECT current_user);
END;
$$ LANGUAGE plpgsql;

DO $$
<<locals>>
DECLARE
    obj         record;
    command     text;
    wanted_acls aclitem[];
    cur_acls    aclitem[];
    creator     text;
BEGIN
    RAISE NOTICE 'Started assigning permissions';

    FOR obj IN
        SELECT
            name,
            nspname,
            kind,
            owner AS ownr,
            acl
        FROM
            public.schemas_tables_funcs
        WHERE
            nspname = ANY(ARRAY['i2b2demodata',
                'i2b2metadata',
                'deapp',
                'searchapp',
                'biomart',
                'tm_cz',
                'tm_lz',
                'tm_wz']) LOOP

        SELECT
            ARRAY[]::aclitem[] ||
            public.assign_permissions_1(
                obj.name,
                obj.nspname,
                obj.kind::char,
                obj.ownr)
            || public.assign_permissions_2(
                obj.name,
                obj.nspname,
                obj.kind::char,
                obj.ownr)
            || public.assign_permissions_3(
                obj.name,
                obj.nspname,
                obj.kind::char,
                obj.ownr)
            INTO wanted_acls;

        SELECT COALESCE(array_accum(v)::aclitem[], ARRAY[]::aclitem[])
        INTO wanted_acls
        FROM UNNEST(wanted_acls) A(v)
        WHERE v IS NOT NULL;

        SELECT COALESCE(obj.acl, ARRAY[]::aclitem[])
        INTO obj.acl;

        IF wanted_acls @> obj.acl AND obj.acl @> wanted_acls THEN
            CONTINUE;
        END IF;

        RAISE NOTICE 'Updating permissions of %.% to % (before: %)',
            obj.nspname, obj.name, wanted_acls, obj.acl;

        IF obj.kind = 's' THEN -- schema
            UPDATE pg_namespace
            SET nspacl = wanted_acls
            WHERE nspname = obj.name;
        ELSIF obj.kind = 'f' THEN -- functions
            UPDATE pg_proc p
            SET proacl = wanted_acls
            FROM pg_namespace n
            WHERE
                p.pronamespace = n.oid
                AND n.nspname = obj.nspname
                AND p.proname = obj.name;
        ELSE
            UPDATE pg_class c
            SET relacl = wanted_acls
            FROM pg_namespace n
            WHERE
                c.relnamespace = n.oid
                AND n.nspname = obj.nspname
                AND c.relname = obj.name;
        END IF;

    END LOOP;

    RAISE NOTICE 'Assigning default permissions';

    FOR obj IN
            SELECT
                nschema,
                ntype,
                array_accum(('user ' || nuser || '=' || nperm || '/'
                    || (SELECT current_user))) as acl
            FROM public.ts_default_permissions
            GROUP BY nschema, ntype LOOP

        wanted_acls := obj.acl;

        FOREACH creator IN ARRAY ARRAY['tm_cz', (SELECT current_user)] LOOP

            SELECT
                defaclacl
            INTO
                cur_acls
            FROM
                pg_default_acl D
                INNER JOIN pg_roles R ON (R.oid = D.defaclrole)
                INNER JOIN pg_namespace N ON (N.oid = D.defaclnamespace)
            WHERE
                rolname = creator
                AND nspname = obj.nschema
                AND defaclobjtype = obj.ntype;

            IF wanted_acls @> cur_acls AND cur_acls @> wanted_acls THEN
                CONTINUE;
            END IF;

            RAISE NOTICE '% default permissions for creator % on schema % for % to % (before: %)',
                CASE FOUND
                    WHEN true THEN 'Updating'
                    ELSE 'Inserting'
                END,
                creator,
                obj.nschema,
                CASE obj.ntype
                    WHEN 'r' THEN 'relations'
                    WHEN 'S' THEN 'sequences'
                    WHEN 'f' THEN 'functions'
                END,
                wanted_acls,
                cur_acls;

            IF FOUND THEN
                UPDATE
                    pg_default_acl D
                SET
                    defaclacl = wanted_acls
                FROM
                    pg_roles R,
                    pg_namespace N
                WHERE
                    R.oid             = D.defaclrole      -- join condition
                    AND N.oid         = D.defaclnamespace -- join condition
                    AND R.rolname     = creator
                    AND N.nspname     = obj.nschema
                    AND defaclobjtype = obj.ntype;
            ELSE
                INSERT INTO pg_default_acl(
                    defaclrole,
                    defaclnamespace,
                    defaclobjtype,
                    defaclacl)
                VALUES(
                    (SELECT oid FROM pg_roles WHERE rolname = creator),
                    (SELECT oid FROM pg_namespace WHERE nspname = obj.nschema),
                    obj.ntype,
                    wanted_acls
                );
            END IF;

        END LOOP;

    END LOOP;

    RAISE NOTICE 'Finished assigning permissions';

END;
$$ LANGUAGE plpgsql;

-- vim: ft=plsql ts=4 sw=4 tw=80 et:
