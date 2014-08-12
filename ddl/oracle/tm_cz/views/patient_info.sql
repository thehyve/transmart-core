--
-- Type: VIEW; Owner: TM_CZ; Name: PATIENT_INFO
--
CREATE OR REPLACE FORCE VIEW "TM_CZ"."PATIENT_INFO" ("STUDY_ID", "SUBJECT_ID", "SITE_ID", "USUBJID") AS 
select TRIAL_NAME as STUDY_ID, SUBJECT_ID, SITE_ID, REGEXP_REPLACE(TRIAL_NAME || ':' || SITE_ID || ':' || SUBJECT_ID,
                 '(::){1,}', ':') as usubjid from tm_cz.stg_subject_rbm_data;
