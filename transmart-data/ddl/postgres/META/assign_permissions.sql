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
-- – Assign misc permissions.

-- {{{ assign_permissions_1
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
-- }}}

-- {{{ assign_permissions_2
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
            WHEN 'a' THEN 'X'
            WHEN 's' THEN 'UC'
            WHEN 'T' THEN 'C'
        END ||
        '/' || (SELECT current_user);

    IF owner IS NOT NULL THEN
        result := ARRAY[
            ('user ' || owner || '=' || allPermissions)::aclitem
        ];
    END IF;
    IF owner <> 'tm_cz' OR owner IS NULL THEN
        result := result || ('user tm_cz=' || allPermissions)::aclitem;
    END IF;

    RETURN result;
END;
$$ LANGUAGE plpgsql;
-- }}}

-- {{{ assign_permissions_3
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
            'searchapp', 'galaxy', 'biomart', 'amapp', 'fmapp', 'biomart_user']) THEN
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
                WHEN 'a' THEN 'X'
                WHEN 's' THEN 'U' -- usage only; not create
            END ||
        '/' || (SELECT current_user);
END;
$$ LANGUAGE plpgsql;
-- }}}

-- {{{ assign_permissions_4
-- Assign misc permissions.
CREATE OR REPLACE FUNCTION public.assign_permissions_4(name text, schema_name text, type char, owner text)
        RETURNS aclitem[] AS $$
DECLARE
    res aclitem[];
BEGIN
    SELECT
        array_accum(
                ('user ' || nuser || '=' || nperm || '/'
                        || (SELECT current_user))::aclitem)
    INTO res
    FROM
        public.ts_misc_permissions
    WHERE
        nschema = schema_name
        AND nname = name
        AND ntype = type;

    RETURN res;
END;
$$ LANGUAGE plpgsql;
-- }}}

-- {{{ grant_aclitems
CREATE OR REPLACE FUNCTION public.grant_aclitems(new_aclitems aclitem[],
        old_aclitems aclitem[], type text, objname text)
        RETURNS void AS $$
DECLARE
    grantee text;
    priv    text;
    command text;
    objtype text;
BEGIN
    objtype :=
        CASE type
            WHEN 'r' THEN 'TABLE'
            WHEN 'v' THEN 'TABLE'
            WHEN 'S' THEN 'SEQUENCE'
            WHEN 'f' THEN 'FUNCTION'
            WHEN 'a' THEN 'FUNCTION' -- It's actually AGGREGATE, but we have to reference to it as FUNCTION in DDL
            WHEN 's' THEN 'SCHEMA'
            WHEN 'T' THEN 'TABLESPACE'
            ELSE 'BAD TYPE: ' || type
        END;

    -- first revoke everything
    FOR grantee IN
            SELECT
                DISTINCT R.rolname
            FROM
                (
                    SELECT (R.rec).*
                    FROM
                        (
                            SELECT
                                aclexplode(old_aclitems)
                        ) AS R (rec)
                ) AS B
                INNER JOIN pg_roles R ON (B.grantee = R.oid) LOOP

        command := 'REVOKE ALL PRIVILEGES ON ' || objtype ||
                ' ' || objname || ' FROM ' || grantee;
        EXECUTE(command);
    END LOOP;

    -- then grant the permissions individually
    FOR grantee, priv IN
            SELECT
                R.rolname, B.privilege_type
            FROM
                (
                    SELECT (R.rec).*
                    FROM
                        (
                            SELECT
                                aclexplode(new_aclitems)
                        ) AS R (rec)
                ) AS B
                INNER JOIN pg_roles R ON (B.grantee = R.oid) LOOP

        command := 'GRANT ' || priv || ' ON ' || objtype ||
                ' ' || objname || ' TO ' || grantee;
        EXECUTE(command);
    END LOOP;
END;
$$ LANGUAGE plpgsql;
-- }}}

-- {{{ equal_aclitem_arrays
-- Compare aclitem arrays ignoring grantor and with grant permission
CREATE OR REPLACE FUNCTION public.equal_aclitem_arrays(aclitems1 aclitem[], aclitems2 aclitem[])
        RETURNS boolean AS $$
DECLARE
    ret boolean;
BEGIN
    -- aclexplode can't handle empty arrays
    IF aclitems1 = '{}'::aclitem[] THEN
        IF aclitems2 = aclitems1 THEN
            RETURN true;
        ELSE
            RETURN false;
        END IF;
    END IF;

    SELECT
        NOT EXISTS (
            SELECT
                A.grantee,
                A.privilege_type,
                COUNT(*)
            FROM
                (
                    SELECT DISTINCT
                        (R.rec).grantee,
                        (R.rec).privilege_type
                    FROM
                        (SELECT ACLEXPLODE(aclitems1)) AS R(rec)

                    UNION ALL

                    SELECT DISTINCT
                        (R.rec).grantee,
                        (R.rec).privilege_type
                    FROM
                        (SELECT ACLEXPLODE(aclitems2)) AS R(rec)
                ) A
            GROUP BY
                A.grantee,
                A.privilege_type
            HAVING
                COUNT(*) <> 2
         )
     INTO ret;

    RETURN ret;
END;
$$ LANGUAGE plpgsql;
-- }}}

-- {{{ main function
DO $$
<<locals>>
DECLARE
    obj         record;
    priv        record;
    command     text;
    wanted_acls aclitem[];
    cur_acls    aclitem[];
    creator     text;
    grantee     text;
    permission  text;
    schemas     text[];
    s           text;
BEGIN
    schemas := ARRAY[
            'i2b2demodata',
            'i2b2metadata',
            'amapp',
            'fmapp',
            'deapp',
            'searchapp',
            'galaxy',
            'biomart',
            'biomart_user',
            'tm_cz',
            'tm_lz',
            'tm_wz',
            'ts_batch'];

    RAISE NOTICE 'Started assigning permissions';

    RAISE NOTICE 'Revoke everything from PUBLIC';
    FOREACH s IN ARRAY schemas LOOP
        command := 'REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA ' ||
                s || ' FROM PUBLIC';
        EXECUTE(command);
        command := 'REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA ' ||
                s || ' FROM PUBLIC';
        EXECUTE(command);
        command := 'REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ' ||
                s || ' FROM PUBLIC';
        EXECUTE(command);
    END LOOP;

    -- {{{ Object permissions
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
            nspname = ANY(schemas) OR nspname IS NULL LOOP

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
            || public.assign_permissions_4(
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

        IF public.equal_aclitem_arrays(obj.acl, wanted_acls) THEN
            CONTINUE;
        END IF;

        RAISE NOTICE 'Updating permissions of %.% to % (before: %)',
            CASE obj.kind
                WHEN 's' THEN 'SCHEMA'
                WHEN 'T' THEN 'TABLESPACE'
                ELSE obj.nspname
            END, obj.name, wanted_acls, obj.acl;

        PERFORM public.grant_aclitems(
            wanted_acls,
            CASE COALESCE(array_length(obj.acl, 1), 0)
                WHEN 0 THEN NULL
                ELSE obj.acl
            END,
            obj.kind,
            CASE obj.kind
                WHEN 's' THEN obj.name
                WHEN 'T' THEN obj.name
                ELSE obj.nspname || '.' || obj.name
            END);

    END LOOP;
    -- }}}

    -- {{{ Default permissions
    RAISE NOTICE 'Assigning default permissions';

    FOREACH creator IN ARRAY ARRAY['tm_cz', (SELECT current_user)] LOOP
        FOR obj IN
                SELECT
                    nschema,
                    ntype,
                    array_accum(('user ' || nuser || '=' || nperm || '/'
                        || creator)::aclitem) as acl
                FROM public.ts_default_permissions
                GROUP BY nschema, ntype LOOP

            wanted_acls := obj.acl;

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

            IF public.equal_aclitem_arrays(cur_acls, wanted_acls) THEN
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

            -- first revoke all default privileges
            FOR grantee IN
                    SELECT
                        DISTINCT R.rolname
                    FROM
                        (
                            SELECT (R.rec).*
                            FROM
                                (
                                    SELECT
                                        aclexplode(wanted_acls)
                                ) AS R (rec)
                        ) AS B
                        INNER JOIN pg_roles R ON (B.grantee = R.oid) LOOP

                command := 'ALTER DEFAULT PRIVILEGES FOR USER ' || creator ||
                        ' IN SCHEMA ' || obj.nschema || ' REVOKE ALL PRIVILEGES' ||
                        ' ON ' ||
                        CASE obj.ntype
                            WHEN 'r' THEN 'TABLES'
                            WHEN 'S' THEN 'SEQUENCES'
                            WHEN 'f' THEN 'FUNCTIONS'
                        END ||
                        ' FROM ' || grantee;
                EXECUTE(command);
            END LOOP;

            -- then assign the new
            FOR priv IN
                    SELECT
                        R.rolname AS grantee,
                        B.privilege_type
                    FROM
                        (
                            SELECT (R.rec).*
                            FROM
                                (
                                    SELECT
                                        aclexplode(wanted_acls)
                                ) AS R(rec)
                        ) AS B
                        INNER JOIN pg_roles R ON (B.grantee = R.oid) LOOP

                    command := 'ALTER DEFAULT PRIVILEGES FOR USER ' || creator ||
                            ' IN SCHEMA ' || obj.nschema || ' GRANT ' ||
                            priv.privilege_type ||
                            ' ON ' ||
                            CASE obj.ntype
                                WHEN 'r' THEN 'TABLES'
                                WHEN 'S' THEN 'SEQUENCES'
                                WHEN 'f' THEN 'FUNCTIONS'
                            END ||
                            ' TO ' || priv.grantee;
                    EXECUTE(command);
            END LOOP;

        END LOOP; -- each schema/type pair

    END LOOP; -- each creator
    -- }}}

    RAISE NOTICE 'Finished assigning permissions';

END;
$$ LANGUAGE plpgsql;
-- }}}

-- vim: ft=plsql ts=4 sw=4 tw=80 et:
