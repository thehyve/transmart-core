--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_RECOMPILE_ALL
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_RECOMPILE_ALL" 
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_RECOMPILE_ALL
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   CURSOR v_proclist IS
     SELECT distinct 'alter '|| object_type || ' ' || object_name || ' compile '
     FROM user_procedures;

   v_procname VARCHAR2(50);

BEGIN

   OPEN v_proclist;
   FETCH v_proclist INTO v_procname;
   WHILE v_proclist%FOUND
   LOOP

      BEGIN
         BEGIN

            BEGIN
               execute immediate v_procname;
               DBMS_OUTPUT.PUT_LINE('succesfully compiled ' || v_procname);
            END;
         EXCEPTION
            WHEN OTHERS THEN

               BEGIN
                  DBMS_OUTPUT.PUT_LINE('error compiling ' || v_procname);
               END;
         END;
         FETCH v_proclist INTO v_procname;
      END;
   END LOOP;
   -- while loop
   CLOSE v_proclist;-- procedure
END;
/
 
