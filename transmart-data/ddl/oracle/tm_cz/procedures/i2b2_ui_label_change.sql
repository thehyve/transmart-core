--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_UI_LABEL_CHANGE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_UI_LABEL_CHANGE" 
(path IN  VARCHAR2
,old_label IN VARCHAR2
, new_label IN VARCHAR2
, rtn_code OUT NUMBER
) AS

--local variables
fullPath varchar2(300);
oldLabel varchar2(200);
newLabel varchar2(200);
newPath  varchar2(300);

pathExist number;
conceptCd varchar2(100);
noLabelCount number;


-- exception handling
missing_path	exception;
--missing_old_label	EXCEPTION;
multiple_path exception;
no_label exception;
 
BEGIN
 
 
 -- assign parameter values to the local variable
 
   fullPath:=path;
  -- newPath:=new_path;
   oldLabel:=old_label;
   newLabel:=new_label;
   
   
 
 -- search the path in concept_dimension table 
 
 select count(*) into pathExist from concept_dimension where concept_path=fullPath;
 

 -- if no path exist raise missing_path
 if pathExist=0
 then 
 raise missing_path;
 end if;
 
 -- if multiple path exist  with same name raise multiple_path
 if pathExist > 1
 
 then
 raise multiple_path;
 end if;
 
 select concept_cd into conceptCd from concept_dimension where concept_path=fullPath;
 
 --update the new path in concept_dimension table
 
 if conceptCd is not null
 then 
 update concept_dimension set name_char=newLabel where concept_cd=conceptCd;
 
 dbms_output.put_line('New Label is updated in concept_dimension table');
 end if;

 commit;
 select count(*) into noLabelCount from i2b2 where c_basecode=conceptCd and c_name=oldLabel;
 
 
 if noLabelCount =1
 then
 update i2b2 set c_name=newLabel  where c_basecode=conceptCd and c_name=oldLabel;
  dbms_output.put_line('New label name is updated in i2b2 table');
 else
 raise no_label;
 end if;
 commit;
 select 0 into rtn_code from dual;
 
 if rtn_code=0 then 
 
 dbms_output.put_line('The new label name has been changed sucessfully');
 
 end if;
 
 
 EXCEPTION
 
    when missing_path then
   dbms_output.put_line('please provide the correct full path name or check this path is correct or not='||fullPath);
   select 11 into rtn_code from dual;
   when multiple_path then 
   dbms_output.put_line('please provide the correct full path name, given name may not be correct='||fullPath);
   select 12 into rtn_code from dual;
   when no_label then 
   dbms_output.put_line('please provide the correct Label name, given name may not be correct='||oldLabel);
   select 13 into rtn_code from dual;
   when others then    
   dbms_output.put_line('please check the parameters which you have provided');
  select 14 into rtn_code from dual;
    
 
 
END;
/
 
