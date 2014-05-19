--
-- Name: util_truncate_table(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION util_truncate_table (
  v_tabname IN character varying DEFAULT NULL ,
  v_dummyarg IN character varying DEFAULT 'Y'::character varying
)
--AUTHID CURRENT_USER
 RETURNS VOID AS $body$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_TRUNCATE_TABLE
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_cmdlist CURSOR IS
     /* disable all foreign key constraints on this table */
     PERFORM 'alter table ' || t1.owner ||'.' || t1.table_name ||' disable constraint '|| t1.constraint_name || '' cmd
     FROM user_constraints t1, user_constraints t2
     WHERE T1.CONSTRAINT_TYPE='R' and T1.R_CONSTRAINT_NAME=T2.CONSTRAINT_NAME and
        T2.TABLE_NAME = v_tabname
     UNION ALL
     /* finally actually truncate the table */
     PERFORM 'truncate table ' || v_tabname || '' cmd
     
     UNION ALL
     /* do a delete just incase the truncate failed */
     PERFORM 'delete from ' || v_tabname || '' cmd
     ;

   v_cmdline varchar(200);
   v_drop_if_populated integer;


BEGIN

   /* done with the SQL select - now process each command we selected */
   OPEN v_cmdlist;
   FETCH v_cmdlist INTO v_cmdline;
   WHILE v_cmdlist%FOUND
   LOOP

      BEGIN
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
         FETCH v_cmdlist INTO v_cmdline;
      END;
   END LOOP;
   -- while loop
   CLOSE v_cmdlist;-- procedure
END;
 
$body$
LANGUAGE PLPGSQL;
