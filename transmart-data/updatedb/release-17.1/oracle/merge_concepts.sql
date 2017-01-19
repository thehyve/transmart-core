--***** Introduce shared concept ****
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE shared_concept';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

CREATE GLOBAL TEMPORARY TABLE shared_concept
   ON COMMIT PRESERVE ROWS 
AS select 2 as "LEVEL", 'MRNA_GPL570' as "CONCEPT_CD", '\Biomarker Data\GPL570\MRNA\' as "CONCEPT_PATH", 'MRNA' as "NAME_CHAR" from dual;

insert into i2b2demodata.concept_dimension(
"CONCEPT_CD",
"CONCEPT_PATH",
"NAME_CHAR"
) select * from shared_concept;

---***** Concept pathes to replace *****
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE concept_pathes_to_replace';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

CREATE GLOBAL TEMPORARY TABLE concept_pathes_to_replace
   ON COMMIT PRESERVE ROWS 
   AS select distinct cd.concept_path from i2b2demodata.concept_dimension cd 
join DEAPP.DE_SUBJECT_SAMPLE_MAPPING sample_ on sample_.concept_code = cd.CONCEPT_CD
where sample_.TRIAL_NAME like 'MS_%'
and cd.concept_path like '%GPL570%'
order by cd.concept_path;

----****** Relink the metadata records to shared concept ******
update i2b2metadata.i2b2_secure set c_dimcode = (select concept_path from shared_concept) where c_dimcode in (select concept_path from concept_pathes_to_replace);
update i2b2metadata.i2b2 set c_dimcode = (select concept_path from shared_concept) where c_dimcode in (select concept_path from concept_pathes_to_replace);

----****** Introduce new root folder to the tree *****
insert into i2b2metadata.table_access(
"C_TABLE_CD", 
"C_TABLE_NAME", 
"C_PROTECTED_ACCESS", 
"C_HLEVEL", 
"C_FULLNAME", 
"C_NAME", 
"C_SYNONYM_CD", 
"C_VISUALATTRIBUTES", 
"C_FACTTABLECOLUMN", 
"C_DIMTABLENAME", 
"C_COLUMNNAME", 
"C_COLUMNDATATYPE", 
"C_OPERATOR", 
"C_DIMCODE")
values(
  'Biomarker Data',
  'I2B2',
  'N',
  0,
  '\Biomarker Data\',
  'Biomarker Data',
  'N',
  'CAE',
  'concept_cd',
  'concept_dimension',
  'concept_path',
  'T',
  'LIKE',
  '\Biomarker Data\'
);

---- **** Add shared concept to the tree *****
insert into i2b2metadata.i2b2(
"C_HLEVEL",
"C_FULLNAME", 
"C_NAME", 
"C_SYNONYM_CD", 
"C_VISUALATTRIBUTES", 
"C_FACTTABLECOLUMN", 
"C_TABLENAME", 
"C_COLUMNNAME", 
"C_COLUMNDATATYPE", 
"C_OPERATOR", 
"C_DIMCODE",  
"UPDATE_DATE" 
) values (
(select "LEVEL" from shared_concept),
(select concept_path from shared_concept),
(select name_char from shared_concept),
'N',
'LAH',
'CONCEPT_CD',
'CONCEPT_DIMENSION',
'CONCEPT_PATH',
'T',
'LIKE',
(select concept_path from shared_concept),
SYSDATE
);

insert into i2b2metadata.i2b2_secure(
"C_HLEVEL",
"C_FULLNAME", 
"C_NAME", 
"C_SYNONYM_CD", 
"C_VISUALATTRIBUTES", 
"C_FACTTABLECOLUMN", 
"C_TABLENAME", 
"C_COLUMNNAME", 
"C_COLUMNDATATYPE", 
"C_OPERATOR", 
"C_DIMCODE",  
"UPDATE_DATE",
"SECURE_OBJ_TOKEN"
) values (
(select "LEVEL" from shared_concept),
(select concept_path from shared_concept),
(select name_char from shared_concept),
'N',
'LAH',
'CONCEPT_CD',
'CONCEPT_DIMENSION',
'CONCEPT_PATH',
'T',
'LIKE',
(select concept_path from shared_concept),
SYSDATE,
'PUBLIC'
);

--- ****** Make clinical data to refer to the shared concept *****
update i2b2demodata.observation_fact obs set concept_cd = (select concept_cd from shared_concept),
instance_num = trial_visit_num 
where obs.concept_cd in (select cd.concept_cd from i2b2demodata.concept_dimension cd inner join concept_pathes_to_replace cpr on cpr.concept_path = cd.concept_path);

--- ****** Make HD records refer to the shared concept ******¯
update deapp.de_subject_sample_mapping ssm set ssm.concept_code = (select concept_cd from shared_concept)
where ssm.concept_code in (select cd.concept_cd from i2b2demodata.concept_dimension cd inner join concept_pathes_to_replace cpr on cpr.concept_path = cd.concept_path);