--
-- Name: rename_program(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION rename_program(oldprogramname character varying, newprogramname character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

oldTopNode		varchar(2000);
newTopNode		varchar(2000);
regex1		varchar(2000);
regex2		varchar(2000);

BEGIN

DBMS_OUTPUT.ENABLE (20000);
  oldTopNode := '\' || oldProgramName || '\' ;
  newTopNode := '\' || newProgramName || '\' ;
  regex1 := '\\' || oldProgramName || '\\' || '(.*)';
  regex2 := '\\' || newProgramName || '\\' || '\1';
  
  update i2b2metadata.i2b2 set c_fullname=REGEXP_REPLACE(c_fullname, regex1, regex2) where c_fullname like oldTopNode||'%';
  update i2b2metadata.i2b2 set c_dimcode=REGEXP_REPLACE(c_dimcode, regex1, regex2) where c_dimcode like oldTopNode||'%';
  update i2b2metadata.i2b2 set c_tooltip=REGEXP_REPLACE(c_tooltip, regex1, regex2)  where c_tooltip like oldTopNode||'%';
  update i2b2metadata.i2b2 set c_name=newProgramName where c_fullname=newTopNode;
  
  update i2b2metadata.i2b2_secure set c_fullname=REGEXP_REPLACE(c_fullname, regex1, regex2) where c_fullname like oldTopNode||'%';
  update i2b2metadata.i2b2_secure set c_dimcode=REGEXP_REPLACE(c_dimcode, regex1, regex2) where c_dimcode like oldTopNode||'%';
  update i2b2metadata.i2b2_secure set c_tooltip=REGEXP_REPLACE(c_tooltip, regex1, regex2) where c_tooltip like oldTopNode||'%';
  update i2b2metadata.i2b2_secure set c_name=newProgramName where c_fullname=newTopNode;
  
  update i2b2metadata.table_access set c_fullname=REGEXP_REPLACE(c_fullname, regex1, regex2) where c_fullname like oldTopNode||'%';
  update i2b2metadata.table_access set c_dimcode=REGEXP_REPLACE(c_dimcode, regex1, regex2) where c_dimcode like oldTopNode||'%';
  update i2b2metadata.table_access set c_tooltip=REGEXP_REPLACE(c_tooltip, regex1, regex2) where c_tooltip like oldTopNode||'%';
  update i2b2metadata.table_access set c_name=newProgramName where c_fullname=newTopNode;
  update i2b2metadata.table_access set c_table_cd=newProgramName where c_fullname=newTopNode;
 
  update I2B2DEMODATA.concept_counts set concept_path=REGEXP_REPLACE(concept_path, regex1, regex2) where concept_path like oldTopNode||'%';
  update I2B2DEMODATA.concept_counts set parent_concept_path=REGEXP_REPLACE(parent_concept_path, regex1, regex2)where parent_concept_path like oldTopNode||'%';
 
  update I2B2DEMODATA.concept_dimension set concept_path=REGEXP_REPLACE(concept_path, regex1, regex2) where concept_path like oldTopNode||'%';

 commit;
 
END;
 
$$;

