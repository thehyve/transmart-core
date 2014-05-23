--
-- Name: util_recompile_all(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_recompile_all() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_RECOMPILE_ALL
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_proclist CURSOR FOR
     SELECT distinct 'alter '|| object_type || ' ' || object_name || ' compile '
     FROM user_procedures;

   v_procname varchar(50);


BEGIN

   OPEN v_proclist;
   FETCH v_proclist INTO v_procname;
   WHILE v_proclist%FOUND
   LOOP

      BEGIN
         BEGIN

            BEGIN
               EXECUTE v_procname;
               RAISE NOTICE '%%', 'succesfully compiled ' ,  v_procname;
            END;
         EXCEPTION
            WHEN OTHERS THEN

               BEGIN
                  RAISE NOTICE '%%', 'error compiling ' ,  v_procname;
               END;
         END;
         FETCH v_proclist INTO v_procname;
      END;
   END LOOP;
   -- while loop
   CLOSE v_proclist;-- procedure
END;
 
$$;

