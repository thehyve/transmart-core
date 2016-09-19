--
-- Name: util_drop_all(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_drop_all(v_whattype character varying DEFAULT 'PROCEDURES,FUNCTIONS,VIEWS,SYNONYMS'::character varying, v_dropifempty character varying DEFAULT 'Y'::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_ALL
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


   l_table CURSOR FOR
     SELECT table_name from user_tables;

   l_view CURSOR FOR
     SELECT view_name from user_views;

   l_procedure CURSOR FOR
     SELECT distinct object_name, object_type from user_procedures;

   l_synonym CURSOR FOR
     SELECT synonym_name from user_synonyms;

   l_constraint CURSOR FOR
     SELECT distinct table_name, constraint_name from user_constraints;


BEGIN

   --util_make_object_list(v_whattype, v_things);

   -- drop procedure(s) or function(s)
   if upper(v_whattype) like 'FUNCTION' or upper(v_whattype) like 'FUNCTION' then
      open l_procedure;
      fetch l_procedure into v_procname, v_objtype;
      while l_procedure%FOUND
      loop
         -- dbms_output.put_line( v_objtype || '  ' || v_procname);
         EXECUTE 'drop '|| v_objtype || ' ' || v_procname;

         fetch l_procedure into v_procname, v_objtype;
      end loop;
      close l_procedure;
   end if;


   -- drop table(s)
   if upper(v_whattype) like 'TABLE' then
      open l_table;
      fetch l_table into v_table_name;
      while l_table%FOUND
      loop
         -- dbms_output.put_line( v_table_name);
         EXECUTE 'drop table '|| v_table_name || ' cascade constraints ';

         fetch l_table into v_table_name;
      end loop;
      close l_table;
   end if;

   -- drop synonyms(s)
   if upper(v_whattype) like 'SYNONYM' then
      open l_synonym;
      fetch l_synonym into v_synonym_name;
      while l_synonym%FOUND
      loop
         -- dbms_output.put_line( v_synonym_name);
         EXECUTE 'drop synonym ' || v_synonym_name ;

         fetch l_synonym into v_synonym_name;
      end loop;
      close l_synonym;
   end if;


   -- drop view(s)
   if upper(v_whattype) like 'VIEW' then
      open l_view;
      fetch l_view into v_view_name;
      while l_view%FOUND
      loop
         -- dbms_output.put_line( v_view_name);
         EXECUTE 'drop view '|| v_table_name ;

         fetch l_view into v_view_name;
      end loop;
      close l_view;
   end if;


   -- drop constraint(s)
   if upper(v_whattype) like 'CONSTRAINT' then
      open l_constraint;
      fetch l_constraint into v_table_name, v_constraint_name;
      while l_constraint%FOUND
      loop
         -- dbms_output.put_line( v_constraint_name);
         EXECUTE 'alter table '|| v_table_name || ' drop constraint '|| v_constraint_name ;

         fetch l_constraint into v_table_name, v_constraint_name;
      end loop;
      close l_constraint;
   end if;

END;
 
$$;

