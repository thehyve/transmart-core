--
-- Name: util_drop_synonym_by_owner(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_drop_synonym_by_owner(v_owner character varying, v_dropifempty character varying DEFAULT 'Y'::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_SYNONYM_BY_OWNER
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_procname varchar(50);
   v_objtype varchar(50);
   v_table_name varchar(50);
   v_view_name varchar(50);
   v_synonym_name varchar(50);
   v_constraint_name varchar(50);


   l_synonym CURSOR FOR
     SELECT synonym_name from all_synonyms
	 where owner = v_owner;



BEGIN

   -- drop synonyms(s)

      open l_synonym;
      fetch l_synonym into v_synonym_name;
      while l_synonym%FOUND
      loop
         -- dbms_output.put_line( v_synonym_name);
         EXECUTE( 'drop synonym ' || v_synonym_name) ;

         fetch l_synonym into v_synonym_name;
      end loop;
      close l_synonym;



END;
 
$$;

