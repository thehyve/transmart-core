--
-- Name: util_grant_execute(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION util_grant_execute (
  v_to_zone IN character varying DEFAULT NULL ,
  v_type IN character varying DEFAULT 'TABLES,VIEWS'
)
--AUTHID CURRENT_USER
 RETURNS VOID AS $body$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_GRANT_EXECUTE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

   v_object_name varchar(50);
   v_cmdline varchar(200);

   l_table CURSOR FOR
        SELECT table_name from all_tables where owner = v_to_zone;

   l_view CURSOR FOR
        SELECT view_name from all_views where owner = v_to_zone;


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
                   EXECUTE v_cmdline;
                   RAISE NOTICE '%%', 'SUCCESS ' ,  v_cmdline;
                END;
             EXCEPTION
                WHEN OTHERS THEN

                   BEGIN
                      RAISE NOTICE '%%', 'ERROR ' ,  v_cmdline;
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
                   EXECUTE v_cmdline;
                   RAISE NOTICE '%%', 'SUCCESS ' ,  v_cmdline;
                END;
             EXCEPTION
                WHEN OTHERS THEN

                   BEGIN
                      RAISE NOTICE '%%', 'ERROR ' ,  v_cmdline;
                   END;
             END;

             FETCH l_view INTO v_object_name;
          END;
       END LOOP;
       CLOSE l_view;
   end if;

END;
 
$body$
LANGUAGE PLPGSQL;
