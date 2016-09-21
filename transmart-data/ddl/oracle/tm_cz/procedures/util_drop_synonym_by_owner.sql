--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_DROP_SYNONYM_BY_OWNER
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_DROP_SYNONYM_BY_OWNER" 
(
  v_owner in varchar2,
  v_dropifempty IN VARCHAR2 DEFAULT 'Y'
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_SYNONYM_BY_OWNER
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_procname VARCHAR2(50);
   v_objtype VARCHAR2(50);
   v_table_name VARCHAR2(50);
   v_view_name VARCHAR2(50);
   v_synonym_name VARCHAR2(50);
   v_constraint_name VARCHAR2(50);


   cursor l_synonym is
     select synonym_name from all_synonyms
	 where owner = v_owner;


BEGIN

   -- drop synonyms(s)

      open l_synonym;
      fetch l_synonym into v_synonym_name;
      while l_synonym%FOUND
      loop
         -- dbms_output.put_line( v_synonym_name);
         execute immediate( 'drop synonym ' || v_synonym_name) ;

         fetch l_synonym into v_synonym_name;
      end loop;
      close l_synonym;



END;
/
 
