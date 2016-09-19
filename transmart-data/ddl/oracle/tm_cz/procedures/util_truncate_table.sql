--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_TRUNCATE_TABLE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_TRUNCATE_TABLE" 
(
  v_tabname IN VARCHAR2 DEFAULT NULL ,
  v_dummyarg IN VARCHAR2 DEFAULT 'Y'
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_TRUNCATE_TABLE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   CURSOR v_cmdlist IS
     /* disable all foreign key constraints on this table */
     SELECT 'alter table ' || t1.owner ||'.' || t1.table_name ||' disable constraint '|| t1.constraint_name || '' cmd
     FROM user_constraints t1, user_constraints t2
     WHERE T1.CONSTRAINT_TYPE='R' and T1.R_CONSTRAINT_NAME=T2.CONSTRAINT_NAME and
        T2.TABLE_NAME = v_tabname
     UNION ALL
     /* finally actually truncate the table */
     SELECT 'truncate table ' || v_tabname || '' cmd
     FROM dual
     UNION ALL
     /* do a delete just incase the truncate failed */
     SELECT 'delete from ' || v_tabname || '' cmd
     FROM dual;

   v_cmdline VARCHAR2(200);
   v_drop_if_populated INTEGER;

BEGIN

   /* done with the SQL select - now process each command we selected */
   OPEN v_cmdlist;
   FETCH v_cmdlist INTO v_cmdline;
   WHILE v_cmdlist%FOUND
   LOOP

      BEGIN
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
         FETCH v_cmdlist INTO v_cmdline;
      END;
   END LOOP;
   -- while loop
   CLOSE v_cmdlist;-- procedure
END;
/
 
