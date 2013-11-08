--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_DROP_TABLE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_DROP_TABLE" 
(
  v_tabname IN VARCHAR2 DEFAULT NULL
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_TABLE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
  v_exists INTEGER;
  v_cmdline VARCHAR2(200);

BEGIN

  --Check if table exists
  select count(*)
  into v_exists
  from user_tables
  where table_name = v_tabname;

  if v_exists > 0 then
     v_cmdline := 'drop table ' || v_tabname;
     EXECUTE IMMEDIATE v_cmdline;
  end if;

END;
/
 
