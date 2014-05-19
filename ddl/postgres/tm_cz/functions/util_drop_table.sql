--
-- Name: util_drop_table(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION util_drop_table (
  v_tabname IN character varying DEFAULT NULL
)
--AUTHID CURRENT_USER
 RETURNS VOID AS $body$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_TABLE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
  v_exists integer;
  v_cmdline varchar(200);


BEGIN

  --Check if table exists
  select count(*)
  into v_exists
  from user_tables
  where table_name = v_tabname;

  if v_exists > 0 then
     v_cmdline := 'drop table ' || v_tabname;
     EXECUTE v_cmdline;
  end if;

END;
 
$body$
LANGUAGE PLPGSQL;
