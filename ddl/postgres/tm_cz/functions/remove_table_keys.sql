--
-- Name: remove_constraints_for_table(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION remove_constraints_for_table(p_schema_name character varying, p_table_name character varying) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
   r record;
   sql_statement text;
BEGIN
   FOR r IN
      SELECT tc.constraint_schema, tc.table_name, tc.constraint_name
      FROM information_schema.table_constraints tc
      WHERE tc.table_catalog = current_database()
      	AND tc.table_schema=p_schema_name
      	AND tc.table_name=p_table_name
	AND tc.constraint_type <> 'CHECK'
    LOOP
    	 sql_statement := 'ALTER TABLE ' || quote_ident(r.constraint_schema) || '.' || quote_ident(r.table_name) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
         EXECUTE sql_statement;
    END LOOP;
    RETURN TRUE;
END
$$;

