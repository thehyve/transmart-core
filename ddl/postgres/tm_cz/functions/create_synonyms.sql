--
-- Name: create_synonyms(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION create_synonyms(fromdb character varying, todb character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

  cTableList CURSOR FOR
    SELECT
      UPPER(owner) AS OWNER,
      UPPER(table_name) AS TABLE_NAME
    from
      all_tables
    order by owner, table_name;

  dbCount bigint;
  sourceDB varchar(200);
  targetDB varchar(200);

  dynamicSQL varchar(2000);


BEGIN
  -------------------------------------------------------------------------------
    --Create or replace Synonyms Point to DB A (TO) From DB B (FROM)
    --Input: From DB, TO DB
    --Output: Nothing
   -- KCR@20090310 - First rev.
   -------------------------------------------------------------------------------

/* CANT READ FROM DBA_TABLESPACES
    --Check that DB's exist
    select count(*) into dbCount from dba_tablespaces where tablespace_name = upper(fromDB);
    if dbCOunt > 1
      then
      dbms_output.put_line('From DB is invalid!: ' || fromDB);
   end if;

    if dbCount > 1
      then
      dbms_output.put_line('TO DB is invalid!: ' || toDB);
   end if;
*/

  sourceDB := UPPER(fromDB);
  targetDB := UPPER(toDB);



    --Loop through full list of results (All table for all schemas)
    for r_cTableList in cTableList
    loop
      --if The current owner(DB) matched the toDB then begin creating Synonyms.
      if r_cTableList.owner = targetDB
        then
        dynamicSQL := 'CREATE or REPLACE  OR REPLACE SYNONYM "' || sourceDB || '"."' || r_cTableList.table_name || '" FOR "' || targetDB || '"."' || r_cTableList.table_name || '"';
        RAISE NOTICE '%', dynamicSQL;
        EXECUTE dynamicSQL;
     end if;
    commit;
    end loop; --Loops through full resultset

END;
 
$$;

