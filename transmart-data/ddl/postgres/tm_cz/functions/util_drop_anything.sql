--
-- Name: util_drop_anything(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_drop_anything(v_objname character varying DEFAULT NULL::character varying, v_objtype character varying DEFAULT NULL::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_DROP_ANYTHING
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
   v_cmdline varchar(100);


BEGIN

   if upper(v_objtype) like 'TABLE%' then
       v_cmdline := 'drop '|| v_objtype || ' '|| v_objname || ' cascade constraint';
   else
       v_cmdline := 'drop '|| v_objtype || ' '|| v_objname;
   end if;

   BEGIN
      EXECUTE v_cmdline;
      RAISE NOTICE '%%', 'SUCCESS ' ,  v_cmdline;
   EXCEPTION
      WHEN OTHERS THEN
         RAISE NOTICE '%%', 'ERROR ' ,  v_cmdline;
   END;
END;
 
$$;

