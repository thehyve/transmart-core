--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_DROP_ANYTHING
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_DROP_ANYTHING" 
(
  v_objname IN VARCHAR2 DEFAULT NULL ,
  v_objtype IN VARCHAR2 DEFAULT NULL
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_ANYTHING
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_cmdline VARCHAR2(100);

BEGIN

   if upper(v_objtype) like 'TABLE%' then
       v_cmdline := 'drop '|| v_objtype || ' '|| v_objname || ' cascade constraint';
   else
       v_cmdline := 'drop '|| v_objtype || ' '|| v_objname;
   end if;

   BEGIN
      execute immediate v_cmdline;
      DBMS_OUTPUT.PUT_LINE('SUCCESS ' || v_cmdline);
   EXCEPTION
      WHEN OTHERS THEN
         DBMS_OUTPUT.PUT_LINE('ERROR ' || v_cmdline);
   END;
END;
/
 
