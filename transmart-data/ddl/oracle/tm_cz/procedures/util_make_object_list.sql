--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_MAKE_OBJECT_LIST
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_MAKE_OBJECT_LIST" 
(
  -- comma-separated list of things,
  v_whattype IN VARCHAR2 DEFAULT NULL,

  -- resolved list of things formatted as a list of quoted strings
  -- but without the enclosing parentheses.
  v_things OUT VARCHAR2
)
AUTHID CURRENT_USER
AS
-------------------------------------------------------------------------------------
-- NAME: UTIL_MAKE_OBJECT_LIST
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------

BEGIN

   v_things := REPLACE(UPPER(v_whattype), 'PROCEDURES', 'P,PC') ;
   v_things := REPLACE(UPPER(v_things), 'PROCEDURE', 'P,PC') ;
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
/
 
