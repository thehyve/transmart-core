--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_GRANT_ALL
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_GRANT_ALL" 
(username	varchar2 := 'DATATRUST'
,V_WHATTYPE IN VARCHAR2 DEFAULT 'PROCEDURES,FUNCTIONS,TABLES,VIEWS,PACKAGES,SEQUENCES')
AUTHID CURRENT_USER
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

    v_user      varchar2(2000) := SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA');
	extTable	int;

  begin

	IF UPPER(V_WHATTYPE) LIKE '%TABLE%' THEN
    dbms_output.put_line('Owner ' || v_user  || '   Grantee ' || username);
    dbms_output.put_line('Tables');

     for L_TABLE in (select table_name from user_tables where table_name not like '%EXTRNL%') LOOP

		select count(*) into extTable
		from all_external_tables
		where owner = v_user
		  and table_name = L_TABLE.table_name;
		   
       --if L_TABLE.table_name like '%EXTRNL%' then
	    if extTable > 0 then
          --grant select only to External tables
          execute immediate 'grant select on ' || L_TABLE.table_name || ' to ' || username;
       
       else
          --Grant full permissions on regular tables  
          execute immediate 'grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username;
          --DBMS_OUTPUT.put_line('grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username);
       end if;
       
     END LOOP; --TABLE LOOP
     end if;
     
	IF UPPER(V_WHATTYPE) LIKE '%VIEW%' THEN
    dbms_output.put_line('Owner ' || v_user  || '   Grantee ' || username);
    dbms_output.put_line('Views');

     for L_VIEW in (select view_name from user_views ) LOOP
          execute immediate 'grant select on ' || L_VIEW.view_name || ' to ' || username;
       
     END LOOP; --TABLE LOOP
 end if;

 IF UPPER(V_WHATTYPE) LIKE '%PROCEDURE%' or UPPER(V_WHATTYPE) LIKE '%FUNCTION%' or UPPER(V_WHATTYPE) LIKE '%PACKAGE%'  THEN
    dbms_output.put_line(chr(10) || 'Procedures, functions and packages');

    for L_PROCEDURE in (select object_name from user_objects where object_type in ('PROCEDURE', 'FUNCTION', 'PACKAGE') )
     LOOP

       execute immediate 'grant execute on ' || L_PROCEDURE.object_name || ' to ' || username;
      -- DBMS_OUTPUT.put_line('grant execute on ' || L_PROCEDURE.object_name || ' to ' || username);

     END LOOP; --PROCEDURE LOOP
  end if;
  
 IF UPPER(V_WHATTYPE) LIKE '%SEQUENCE%'  THEN
    dbms_output.put_line(chr(10) || 'Sequence');

    for L_SEQUENCE in (select object_name from user_objects where object_type = 'SEQUENCE' )
     LOOP

       execute immediate 'grant select on ' || L_SEQUENCE.object_name || ' to ' || username;
      -- DBMS_OUTPUT.put_line('grant select on ' || L_SEQUENCE.object_name || ' to ' || username);

     END LOOP; --SEQUENCE LOOP
  end if;

END;
/
 
