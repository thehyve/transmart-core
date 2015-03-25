--
-- Name: remove_table_indexes(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION remove_table_indexes(p_schema_name character varying, p_table_name character varying) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
   r record;
   sql_statement text;
BEGIN
   FOR r IN
      	select distinct
    		ni.nspname, i.relname
		from
    		pg_class t
    		inner join pg_index ix on t.oid = ix.indrelid
    		inner join pg_class i on i.oid = ix.indexrelid
    		inner join pg_namespace ni on ni.oid = i.relnamespace
    		inner join pg_namespace nt on nt.oid = t.relnamespace
		where t.relkind = 'r' and t.relname = p_table_name and nt.nspname = p_schema_name
    LOOP
    	 sql_statement := 'DROP INDEX IF EXISTS' || quote_ident(r.nspname) || '.' || quote_ident(r.relname);
         EXECUTE sql_statement;
    END LOOP;
    RETURN TRUE;
END
$$;

