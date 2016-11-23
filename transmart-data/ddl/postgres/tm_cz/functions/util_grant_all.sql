--
-- Name: util_grant_all(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_grant_all(username character varying DEFAULT 'DATATRUST'::character varying, v_whattype character varying DEFAULT 'PROCEDURES,FUNCTIONS,TABLES,VIEWS,PACKAGES'::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_GRANT_ALL
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

    --GRANTS DATATRUST POSSIBLE PERMISSIONS
    --ON OBJECTS OWNED BY THE CURRENT USER

	--	JEA@20110901	Added parameter to allow username other than DATATRUST, look for EXTRNL as external table names

    v_user      text2(2000) := SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA');

  
BEGIN

	IF UPPER(V_WHATTYPE) LIKE '%TABLE%' THEN
    RAISE NOTICE '%%%%', 'Owner ' ,  v_user  ,  '   Grantee ' ,  username;
    RAISE NOTICE 'Tables';

     for L_TABLE in (select table_name from user_tables where table_name not like '%EXTRNL%') LOOP

       if L_TABLE.table_name like '%EXTRNL%' then
          --grant select only to External tables
          EXECUTE 'grant select on ' || L_TABLE.table_name || ' to ' || username;

       else
          --Grant full permissions on regular tables
          EXECUTE 'grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username;
          --DBMS_OUTPUT.put_line('grant select, insert, update, delete on ' || L_TABLE.table_name || ' to ' || username);
       end if;

     END LOOP; --TABLE LOOP
     end if;

	IF UPPER(V_WHATTYPE) LIKE '%VIEW%' THEN
    RAISE NOTICE '%%%%', 'Owner ' ,  v_user  ,  '   Grantee ' ,  username;
    RAISE NOTICE 'Views';

     for L_VIEW in (select view_name from user_views ) LOOP
          EXECUTE 'grant select on ' || L_VIEW.view_name || ' to ' || username;

     END LOOP; --TABLE LOOP
 end if;

 IF UPPER(V_WHATTYPE) LIKE '%FUNCTION%' or UPPER(V_WHATTYPE) LIKE '%FUNCTION%' or UPPER(V_WHATTYPE) LIKE '%PACKAGE%' THEN
    RAISE NOTICE '%%', chr(10) ,  'Procedures, functions and packages';

    for L_PROCEDURE in (select object_name from user_objects where object_type in ('FUNCTION', 'FUNCTION', 'PACKAGE') )
     LOOP

       EXECUTE 'grant execute on ' || L_PROCEDURE.object_name || ' to ' || username;
      -- DBMS_OUTPUT.put_line('grant execute on ' || L_PROCEDURE.object_name || ' to ' || username);

     END LOOP; --PROCEDURE LOOP
  end if;

END;
 
$$;

