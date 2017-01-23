--
-- Name: util_make_object_list(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION util_make_object_list(v_whattype character varying DEFAULT NULL::character varying, OUT v_things character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_MAKE_OBJECT_LIST
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------


BEGIN

   v_things := REPLACE(UPPER(v_whattype), 'PROCEDURES', 'P,PC') ;
   v_things := REPLACE(UPPER(v_things), 'FUNCTION', 'P,PC') ;
   v_things := REPLACE(UPPER(v_things), 'CONSTRAINTS', 'PK,F') ;
   v_things := REPLACE(UPPER(v_things), 'CONSTRAINT', 'PK,F') ;
   v_things := REPLACE(UPPER(v_things), 'FUNCTIONS', 'FN') ;
   v_things := REPLACE(UPPER(v_things), 'FUNCTION', 'FN') ;
   v_things := REPLACE(UPPER(v_things), 'TABLES', 'U') ;
   v_things := REPLACE(UPPER(v_things), 'TABLE', 'U') ;
   v_things := REPLACE(UPPER(v_things), 'VIEWS', 'V') ;
   v_things := REPLACE(UPPER(v_things), 'VIEW', 'V') ;

   -- add more common names for things
   -- but now transform a,b into 'a','b'
   v_things := REPLACE(UPPER(v_things), ',', ''',''') ;
   v_things := '''' || v_things || '''' ;
END;
 
$$;

