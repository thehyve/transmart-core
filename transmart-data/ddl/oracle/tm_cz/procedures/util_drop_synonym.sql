--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_DROP_SYNONYM
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_DROP_SYNONYM" 
(
  v_objname IN VARCHAR2 DEFAULT NULL
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_SYNONYM
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_cmdline VARCHAR2(100);

   cursor ts is
     select 'drop synonym ' || synonym_name || ' ' from user_synonyms;


BEGIN

  OPEN ts;
   FETCH ts INTO v_cmdline;
   WHILE ts%FOUND
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
         FETCH ts INTO v_cmdline;
      END;
   END LOOP;
   CLOSE ts;
END;
/
 
