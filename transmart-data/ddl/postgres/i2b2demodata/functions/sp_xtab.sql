--
-- Name: sp_xtab(character varying, integer, character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION sp_xtab(v_variable character varying, v_protocol integer, v_subject character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE


/******************************************************************************
   NAME:       sf_xtab
   PURPOSE:    This function serves to flatten the SAS to Oracle Conversion via
               the SLM Process. 

   REVISIONS:
   Ver        Date        Author           Description
   ---------  ----------  ---------------  ------------------------------------
   1.0        5/14/2009   George Kuebrich

   NOTES:

   Automatically available Auto Replace Keywords:
      Object Name:     sf_xtab
      Sysdate:         5/14/2009
      Date and Time:   5/14/2009, 8:33:45 AM, and 5/14/2009 8:33:45 AM
      Username:         (set in TOAD Options, Procedure Editor)
      Table Name:       (set in the "New PL/SQL Object" dialog)

******************************************************************************/
sql_stmt varchar(4000);

BEGIN
   --tmpVar := null;
   sql_stmt := 'select distinct value into STRICT  tmpVar
     from sideshow_eav a,
          protocol b,
          variable c
    where a.protocol_id=b.protocol_id
      and a.protocol_id=c.protocol_id
      and a.protocol_id='||v_protocol|| 
    ' and a.variable_id=c.variable_id
      and a.subject_id='||v_subject||
    ' and c.variable_name in ('''||v_variable||''')';
      
   RAISE NOTICE '%', SQL_STMT;
  
   --RETURN tmpVar;
    EXCEPTION
     WHEN NO_DATA_FOUND THEN
       NULL;
     WHEN OTHERS THEN
       -- Consider logging the error and then re-raise
       RAISE;
END sp_xtab; 
 
$$;

