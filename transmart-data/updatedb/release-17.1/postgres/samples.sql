insert into i2b2demodata.modifier_dimension(
  modifier_path,
  modifier_cd,
  name_char
) values ('\\Samples\\Sample Codes\\', 'TRANSMART:SMPL', 'Sample Codes');

 --delete from i2b2demodata.observation_fact where modifier_cd = 'TRANSMART:SMPL';

insert into I2B2DEMODATA.OBSERVATION_FACT(
   ENCOUNTER_NUM,
   PATIENT_NUM,
   CONCEPT_CD,
   PROVIDER_ID,
   START_DATE,
   MODIFIER_CD,
   INSTANCE_NUM,
   TRIAL_VISIT_NUM,
   VALTYPE_CD,
   TVAL_CHAR,
   LOCATION_CD,
   SOURCESYSTEM_CD
)
select 
  o.encounter_num as ENCOUNTER_NUM,
  o.patient_num as PATIENT_NUM,
  o.concept_cd as CONCEPT_CD,
  o.provider_id as PROVIDER_ID,
  o.start_date as START_DATE,
  'TRANSMART:SMPL' as MODIFIER_CD,
  o.instance_num INSTANCE_NUM,
  o.trial_visit_num as TRIAL_VISIT_NUM,
  'T' as VALTYPE_CD,
  o.sample_cd as TVAL_CHAR,
  o.location_cd as LOCATION_CD,
  o.sourcesystem_cd as SOURCESYSTEM_CD
from i2b2demodata.observation_fact o
where sample_cd is not null;