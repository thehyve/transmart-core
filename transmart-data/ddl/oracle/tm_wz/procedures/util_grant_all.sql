--
-- Type: PROCEDURE; Owner: TM_WZ; Name: UTIL_GRANT_ALL
--
  CREATE OR REPLACE PROCEDURE "TM_WZ"."UTIL_GRANT_ALL" 
(username	varchar2 := 'DATATRUST')
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_GRANT_ALL
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

    --GRANTS DATATRUST POSSIBLE PERMISSIONS
    --ON OBJECTS OWNED BY THE CURRENT USER
	
	--	JEA@20110901	Added parameter to allow username other than DATATRUST, look for EXTRNL as external table names

    v_user      varchar2(2000) := SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA');

  begin

    dbms_output.put_line('Owner ' || v_user  || '   Grantee ' || username);
    dbms_output.put_line('Tables');

     for L_TABLE in (select table_name from user_tables where table_name not like '%EXTRNL%') LOOP

       if L_TABLE.table_name like '%EXTRNL%' then
          --grant select only to External tables
          execute immediate 'grant select on ' || L_TABLE.table_name || ' to ' || username;
       
       else
          --Grant full permissions on regular tables  
          execute immediate 'grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username;
          --DBMS_OUTPUT.put_line('grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username);
       end if;
       
     END LOOP; --TABLE LOOP

    dbms_output.put_line(chr(10) || 'Procedures, functions and packages');

    for L_PROCEDURE in (select object_name from user_objects where object_type in ('PROCEDURE', 'FUNCTION', 'PACKAGE') )
     LOOP

       execute immediate 'grant execute on ' || L_PROCEDURE.object_name || ' to ' || username;
      -- DBMS_OUTPUT.put_line('grant execute on ' || L_PROCEDURE.object_name || ' to ' || username);

     END LOOP; --PROCEDURE LOOP

END;
/
