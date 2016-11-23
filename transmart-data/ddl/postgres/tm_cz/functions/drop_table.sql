--
-- Name: drop_table(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION drop_table(tabowner character varying, tabname character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
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

$$;

