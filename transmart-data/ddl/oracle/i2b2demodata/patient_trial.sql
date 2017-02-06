--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: PATIENT_TRIAL
--
 CREATE TABLE "I2B2DEMODATA"."PATIENT_TRIAL" 
  (	"PATIENT_NUM" NUMBER, 
"TRIAL" VARCHAR2(30 BYTE), 
"SECURE_OBJ_TOKEN" VARCHAR2(50 BYTE)
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.patient_trial IS 'Table that links patients to trials. Unused.';

COMMENT ON COLUMN patient_trial.patient_num IS 'Id that links to the patient_num of the patient_dimension table.';
COMMENT ON COLUMN patient_trial.trial IS 'Name of the trial.';
COMMENT ON COLUMN patient_trial.secure_obj_token IS 'Token referring to bio_data_unique_id in searchapp.search_secure_object. E.g., PUBLIC or EXP:GSE8581.';