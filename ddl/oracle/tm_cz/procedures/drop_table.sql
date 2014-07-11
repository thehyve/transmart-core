--
-- Type: PROCEDURE; Owner: TM_CZ; Name: DROP_TABLE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."DROP_TABLE" (
  TabOwner in VARCHAR2,
  TabName in VARCHAR2
  )
  IS
    temp number:=0;
    drp_stmt VARCHAR2 (200):=null;

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
        EXECUTE IMMEDIATE drp_stmt;
        commit;
      end if;

    EXCEPTION
      WHEN OTHERS THEN
      raise_application_error(-20001,'An error was encountered - '||SQLCODE||' -ERROR- '||SQLERRM);

END DROP_TABLE;







/
 
