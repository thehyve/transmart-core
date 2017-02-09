--
-- Type: TABLE; Owner: DEAPP; Name: DE_SUBJECT_SAMPLE_MAPPING
--
 CREATE TABLE "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" 
  (	"PATIENT_ID" NUMBER(18,0), 
"SITE_ID" NVARCHAR2(100), 
"SUBJECT_ID" NVARCHAR2(100), 
"SUBJECT_TYPE" NVARCHAR2(100), 
"CONCEPT_CODE" VARCHAR2(1000 CHAR), 
"ASSAY_ID" NUMBER(18,0) NOT NULL ENABLE, 
"PATIENT_UID" VARCHAR2(50 BYTE), 
"SAMPLE_TYPE" VARCHAR2(100 BYTE), 
"ASSAY_UID" NVARCHAR2(100), 
"TRIAL_NAME" VARCHAR2(30 BYTE), 
"TIMEPOINT" VARCHAR2(100 BYTE), 
"TIMEPOINT_CD" VARCHAR2(50 BYTE), 
"SAMPLE_TYPE_CD" VARCHAR2(50 BYTE), 
"TISSUE_TYPE_CD" VARCHAR2(50 BYTE), 
"PLATFORM" VARCHAR2(50 BYTE), 
"PLATFORM_CD" VARCHAR2(50 BYTE), 
"TISSUE_TYPE" VARCHAR2(100 BYTE), 
"DATA_UID" VARCHAR2(100 BYTE), 
"GPL_ID" VARCHAR2(50 BYTE), 
"RBM_PANEL" VARCHAR2(50 BYTE), 
"SAMPLE_ID" NUMBER(22,0), 
"SAMPLE_CD" VARCHAR2(200 BYTE), 
"CATEGORY_CD" VARCHAR2(1000 BYTE), 
"SOURCE_CD" VARCHAR2(200 BYTE), 
"OMIC_SOURCE_STUDY" VARCHAR2(200 BYTE), 
"OMIC_PATIENT_NUM" NUMBER(18,0), 
"OMIC_PATIENT_ID" NUMBER(18,0),
"PARTITION_ID" NUMBER(18,0)
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: DEAPP; Name: IDX_DE_SUBJ_SMPL_TRIAL_CCODE
--
CREATE INDEX "DEAPP"."IDX_DE_SUBJ_SMPL_TRIAL_CCODE" ON "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" ("TRIAL_NAME", "CONCEPT_CODE")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: DEAPP; Name: DE_SUBJECT_SMPL_MPNG_IDX3
--
CREATE BITMAP INDEX "DEAPP"."DE_SUBJECT_SMPL_MPNG_IDX3" ON "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" ("SAMPLE_TYPE_CD")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: DEAPP; Name: DE_SUBJECT_SMPL_MPNG_IDX2
--
CREATE INDEX "DEAPP"."DE_SUBJECT_SMPL_MPNG_IDX2" ON "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" ("PATIENT_ID", "TIMEPOINT_CD", "PLATFORM_CD", "ASSAY_ID", "TRIAL_NAME")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: DEAPP; Name: DE_SUBJECT_SMPL_MPNG_IDX1
--
CREATE INDEX "DEAPP"."DE_SUBJECT_SMPL_MPNG_IDX1" ON "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" ("TIMEPOINT", "PATIENT_ID", "TRIAL_NAME")
TABLESPACE "TRANSMART" ;

--
-- add documentation
--
COMMENT ON TABLE deapp.de_subject_sample_mapping IS 'Table to store information about assays, which are associated with high dimensional observations. Assays are typically associated with a patient. There can be many assays per patient, which may be distinguished by sample code, tissue type, platform, etc.';

COMMENT ON COLUMN de_subject_sample_mapping.assay_id IS ' Used as primary key of this table (although it is not an actual primary key and there is not even a proper index for this column). This key is references by high dimensional data tables, like de_subject_microarray_data and de_rnaseq_transcript_data.';
COMMENT ON COLUMN de_subject_sample_mapping.patient_id IS 'The patient id linking the patient_dimension. Should not be empty, although it is nullable.';
COMMENT ON COLUMN de_subject_sample_mapping.subject_id IS ' Corresponds to a part of the sourcesystem_cd column of patient_dimension. The patient_id column should be used for properly referencing patients.';
COMMENT ON COLUMN de_subject_sample_mapping.concept_code IS 'Refers to concept_cd in concept_dimension. E.g., CTHD:HD:EXPLUNG.';
COMMENT ON COLUMN de_subject_sample_mapping.trial_name IS 'Name of the trial this sample is part of. Not used.';
COMMENT ON COLUMN de_subject_sample_mapping.gpl_id IS 'Id of the GPL platform for this sample. Links to de_gpl_info table.';
COMMENT ON COLUMN de_subject_sample_mapping.sample_cd IS ' Code to distinguish different samples for the same patient.';