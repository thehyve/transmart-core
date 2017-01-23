--
-- Type: PROCEDURE; Owner: TM_CZ; Name: RENAME_STUDY
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."RENAME_STUDY" (programName IN VARCHAR2, oldTitle IN VARCHAR2, newTitle IN VARCHAR2)
IS
oldTopNode		VARCHAR2(2000);
newTopNode		VARCHAR2(2000);
regex1		VARCHAR2(2000);
regex2		VARCHAR2(2000);
BEGIN

DBMS_OUTPUT.ENABLE (20000);
  oldTopNode := '\' || programName || '\' || oldTitle|| '\';
  newTopNode := '\' || programName || '\' || newTitle|| '\';
  regex1 := '\\' || replace(replace(programName, '(', '\('), ')', '\)') || '\\' || replace(replace(oldTitle, '(', '\('), ')', '\)')|| '\\' || '(.*)';
  regex2 := '\\' || programName || '\\' || newTitle|| '\\' || '\1';
  
  update i2b2metadata.i2b2 set c_fullname=REGEXP_REPLACE(c_fullname, regex1, regex2);
  update i2b2metadata.i2b2 set c_dimcode=REGEXP_REPLACE(c_dimcode, regex1, regex2);
  update i2b2metadata.i2b2 set c_tooltip=REGEXP_REPLACE(c_tooltip, regex1, regex2);
  update i2b2metadata.i2b2 set c_name=newTitle where c_fullname=newTopNode;
  
  update i2b2metadata.i2b2_secure set c_fullname=REGEXP_REPLACE(c_fullname, regex1, regex2);
  update i2b2metadata.i2b2_secure set c_dimcode=REGEXP_REPLACE(c_dimcode, regex1, regex2);
  update i2b2metadata.i2b2_secure set c_tooltip=REGEXP_REPLACE(c_tooltip, regex1, regex2);
  update i2b2metadata.i2b2_secure set c_name=newTitle where c_fullname=newTopNode;
 
  update I2B2DEMODATA.concept_counts set concept_path=REGEXP_REPLACE(concept_path, regex1, regex2);
  update I2B2DEMODATA.concept_counts set parent_concept_path=REGEXP_REPLACE(parent_concept_path, regex1, regex2);
 
  update I2B2DEMODATA.concept_dimension set concept_path=REGEXP_REPLACE(concept_path, regex1, regex2);
  update I2B2DEMODATA.concept_dimension set name_char=newTitle where concept_path=newTopNode;
  
  commit;
 
END;
/
 
