--
-- Name: util_make_object_list(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION util_make_object_list (
  -- comma-separated list of things,
  v_whattype IN character varying DEFAULT NULL,

  -- resolved list of things formatted as a list of quoted strings
  -- but without the enclosing parentheses.
  v_things OUT character varying
)
--AUTHID CURRENT_USER
 RETURNS character varying AS $body$
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
 
$body$
LANGUAGE PLPGSQL;
