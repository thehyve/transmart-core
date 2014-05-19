--
-- Name: drop_table(text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION drop_table (
  TabOwner in character varying,
  TabName in character varying
  )
   RETURNS VOID AS $body$
DECLARE

    temp integer:=0;
    drp_stmt varchar(200):=null;

    
BEGIN
      select
        count(*)
      into
        temp
      from
        all_tables
      where
        upper(TABLE_NAME) = upper(TabName)
      and
        upper(OWNER) = upper(TabOwner);

      if temp = 1 then
        drp_stmt := 'Drop Table ' || TabOwner || '.' || TabName;
        EXECUTE drp_stmt;
        commit;
      end if;

    EXCEPTION
      WHEN OTHERS THEN
      RAISE EXCEPTION 'An error was encountered - % -ERROR- %',SQLSTATE,SQLERRM;

END DROP_TABLE;

$body$
LANGUAGE PLPGSQL;
