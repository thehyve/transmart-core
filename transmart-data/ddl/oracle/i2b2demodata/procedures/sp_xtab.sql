--
-- Type: PROCEDURE; Owner: I2B2DEMODATA; Name: SP_XTAB
--
  CREATE OR REPLACE PROCEDURE "I2B2DEMODATA"."SP_XTAB" (v_variable IN varchar2,
                                    v_protocol IN number,
                                    v_subject  IN varchar2) IS

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
sql_stmt varchar2(4000);
BEGIN
   --tmpVar := null;
   sql_stmt := 'select distinct value into tmpVar
     from sideshow_eav a,
          protocol b,
          variable c
    where a.protocol_id=b.protocol_id
      and a.protocol_id=c.protocol_id
      and a.protocol_id='||v_protocol|| 
    ' and a.variable_id=c.variable_id
      and a.subject_id='||v_subject||
    ' and c.variable_name in ('''||v_variable||''')';
      
   DBMS_OUTPUT.PUT_LINE(SQL_STMT);
  
   --RETURN tmpVar;
   EXCEPTION
     WHEN NO_DATA_FOUND THEN
       NULL;
     WHEN OTHERS THEN
       -- Consider logging the error and then re-raise
       RAISE;
END sp_xtab; 
 
 
 
 
 
 
 
/
 
