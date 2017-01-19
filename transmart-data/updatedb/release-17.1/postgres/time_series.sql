CREATE TEMP TABLE concept_specific_trials AS SELECT
  distinct
  s.study_num as STUDY_NUM,
  (xpath('//SeriesMeta/DisplayName/text()', i.c_metadataxml::xml))[1]::text as REL_TIME_LABEL,
  (xpath('//SeriesMeta/Value/text()', i.c_metadataxml::xml))[1]::text::integer as REL_TIME_NUM,
  (xpath('//SeriesMeta/Unit/text()', i.c_metadataxml::xml))[1]::text as REL_TIME_UNIT_CD,
  c.concept_cd as CONCEPT_CD
FROM I2B2METADATA.I2B2 i
INNER JOIN I2B2DEMODATA.STUDY s ON s.study_id = i.sourcesystem_cd
INNER JOIN I2B2DEMODATA.CONCEPT_DIMENSION c on c.CONCEPT_PATH = i.C_FULLNAME
WHERE i.C_METADATAXML IS NOT NULL
  and i.C_METADATAXML LIKE '%SeriesMeta%';

INSERT INTO I2B2DEMODATA.TRIAL_VISIT_DIMENSION(
  STUDY_NUM,
  REL_TIME_UNIT_CD,
  REL_TIME_NUM,
  REL_TIME_LABEL
) SELECT 
  cst.STUDY_NUM,
  cst.REL_TIME_UNIT_CD,
  cst.REL_TIME_NUM,
  cst.REL_TIME_LABEL
FROM concept_specific_trials cst;

UPDATE I2B2DEMODATA.OBSERVATION_FACT SET TRIAL_VISIT_NUM = (
  select TRIAL_VISIT_NUM from I2B2DEMODATA.TRIAL_VISIT_DIMENSION tv
  inner join concept_specific_trials cst on cst.study_num = tv.study_num
  and cst.REL_TIME_NUM = tv.REL_TIME_NUM
  and cst.REL_TIME_LABEL = tv.REL_TIME_LABEL
  and cst.REL_TIME_UNIT_CD = tv.REL_TIME_UNIT_CD)
WHERE concept_cd in (select concept_cd from concept_specific_trials);