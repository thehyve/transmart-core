--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_GRANT_EXECUTE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_GRANT_EXECUTE" 
(
  v_to_zone IN VARCHAR2 DEFAULT NULL ,
  v_type IN VARCHAR2 DEFAULT 'TABLES,VIEWS'
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_GRANT_EXECUTE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

   v_object_name VARCHAR2(50);
   v_cmdline VARCHAR2(200);

   cursor l_table is
        select table_name from all_tables where owner = v_to_zone;

   cursor l_view is
        select view_name from all_views where owner = v_to_zone;

BEGIN

   if upper(v_type) like '%TABLE%' then
       OPEN l_table;
       FETCH l_table INTO v_object_name;
       WHILE l_table%FOUND
       LOOP
          BEGIN
             v_cmdline := 'grant select on ' || v_object_name || ' to ' || v_to_zone;

             BEGIN

                BEGIN
                   EXECUTE IMMEDIATE v_cmdline;
                   DBMS_OUTPUT.PUT_LINE('SUCCESS ' || v_cmdline);
                END;
             EXCEPTION
                WHEN OTHERS THEN

                   BEGIN
                      DBMS_OUTPUT.PUT_LINE('ERROR ' || v_cmdline);
                   END;
             END;

             FETCH l_table INTO v_object_name;
          END;
       END LOOP;
       CLOSE l_table;
   end if;

   if upper(v_type) like '%VIEW%' then
       OPEN l_view;
       FETCH l_view INTO v_object_name;
       WHILE l_view%FOUND
       LOOP
          BEGIN

             v_cmdline := 'grant select on ' || v_object_name || ' to ' || v_to_zone;

             BEGIN

                BEGIN
                   EXECUTE IMMEDIATE v_cmdline;
                   DBMS_OUTPUT.PUT_LINE('SUCCESS ' || v_cmdline);
                END;
             EXCEPTION
                WHEN OTHERS THEN

                   BEGIN
                      DBMS_OUTPUT.PUT_LINE('ERROR ' || v_cmdline);
                   END;
             END;

             FETCH l_view INTO v_object_name;
          END;
       END LOOP;
       CLOSE l_view;
   end if;

END;
/
 
