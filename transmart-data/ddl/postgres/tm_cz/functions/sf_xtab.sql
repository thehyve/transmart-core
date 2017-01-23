--
-- Name: sf_xtab(character varying, numeric, character varying, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION sf_xtab(v_variable character varying, v_protocol numeric, v_subject character varying, v_rowid bigint) RETURNS character varying
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
sqlstmt varchar(1000);
tmpVar varchar(500);

BEGIN
   
   tmpVar := null;
   EXECUTE 'select distinct value into STRICT  tmpVar
     from sideshow_eav a,
          protocol b,
          variable c
    where a.protocol_id=b.protocol_id
      and a.protocol_id=c.protocol_id
      and a.protocol_id=2 
      and a.variable_id=c.variable_id
      and a.subject_id=3
      and c.variable_name in (4)
      and a.row_id =5' USING v_protocol,v_subject,v_variable,v_rowid;
    
    --EXECUTE IMMEDIATE sqlstmt USING v_protocol,v_subject,v_variable;
  
   RETURN tmpVar;
    EXCEPTION
     WHEN NO_DATA_FOUND THEN
       NULL;
     
END sf_xtab; 
 
$$;

