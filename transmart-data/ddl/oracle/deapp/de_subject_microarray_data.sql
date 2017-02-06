--
-- Type: TABLE; Owner: DEAPP; Name: DE_SUBJECT_MICROARRAY_DATA
--
 CREATE TABLE "DEAPP"."DE_SUBJECT_MICROARRAY_DATA" 
  (	"TRIAL_NAME" VARCHAR2(50 BYTE), 
"PROBESET_ID" NUMBER(22,0), 
"ASSAY_ID" NUMBER(18,0), 
"PATIENT_ID" NUMBER(18,0), 
"SAMPLE_ID" NUMBER(18,0), 
"SUBJECT_ID" VARCHAR2(50 BYTE), 
"RAW_INTENSITY" NUMBER(18,4), 
"LOG_INTENSITY" NUMBER(18,4), 
"ZSCORE" NUMBER(18,4), 
"NEW_RAW" NUMBER(18,4), 
"NEW_LOG" NUMBER(18,4), 
"NEW_ZSCORE" NUMBER(18,4), 
"TRIAL_SOURCE" VARCHAR2(200 BYTE),
"PARTITION_ID" NUMBER(18,0),
 CONSTRAINT "DE_SUBJECT_MICROARRAY_DATA_PK" PRIMARY KEY ("ASSAY_ID", "PROBESET_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;


--
-- add documentation
--
COMMENT ON TABLE deapp.de_subject_microarray_data IS 'Table holds microarray data values.';

COMMENT ON COLUMN de_subject_microarray_data.trial_name IS 'Name of the trial. E.g., SHARED_HD_CONCEPTS_STUDY_C_PR. Not used.';
COMMENT ON COLUMN de_subject_microarray_data.assay_id IS 'Id used to link highdim data to assays in the de_subject_sample_mapping table.';
COMMENT ON COLUMN de_subject_microarray_data.patient_id IS 'The patient id linking to the patient_dimension.';
COMMENT ON COLUMN de_subject_microarray_data.raw_intensity IS 'Raw projection.';
COMMENT ON COLUMN de_subject_microarray_data.log_intensity IS 'Log projection.';
COMMENT ON COLUMN de_subject_microarray_data.zscore IS 'Zscore projection.';