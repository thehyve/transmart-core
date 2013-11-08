--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_GRANT_SELECT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_GRANT_SELECT" 
(username	varchar2 := 'DATATRUST')
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_GRANT_ALL
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

    --GRANTS SELECT PERMISSIONS to DATATRUST (default) or specified user
    --ON OBJECTS OWNED BY THE CURRENT USER

	--	JEA@20110927	New, cloned from UTIL_GRANT_ALL

    v_user      varchar2(2000) := SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA');

  begin

    dbms_output.put_line('Owner ' || v_user  || '   Grantee ' || username);
    dbms_output.put_line('Tables');

     for L_TABLE in (select table_name from user_tables) LOOP

	 execute immediate 'grant select on ' || L_TABLE.table_name || ' to ' || username;
          DBMS_OUTPUT.put_line('grant select on ' || L_TABLE.table_name || ' to ' || username);

     END LOOP; --TABLE LOOP

  --  dbms_output.put_line(chr(10) || 'Views');

 --   for L_VIEW in (select object_name from user_objects where object_type = 'VIEW' )
  --   LOOP

   --    execute immediate 'grant select on ' || L_VIEW.object_name || ' to ' || username;
   --    DBMS_OUTPUT.put_line('grant select on ' || L_VIEW.object_name || ' to ' || username);

   --  END LOOP; --VIEW LOOP

END;
/
 
