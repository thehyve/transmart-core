--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: OBSERVATION_FACT
--
 CREATE TABLE "I2B2DEMODATA"."OBSERVATION_FACT" 
  (	"ENCOUNTER_NUM" NUMBER(38,0), 
"PATIENT_NUM" NUMBER(38,0) NOT NULL ENABLE, 
"CONCEPT_CD" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
"PROVIDER_ID" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
"START_DATE" DATE, 
"MODIFIER_CD" VARCHAR2(100 BYTE) NOT NULL ENABLE, 
"INSTANCE_NUM" NUMBER(18,0), 
"TRIAL_VISIT_NUM" NUMBER(38,0), 
"VALTYPE_CD" VARCHAR2(50 BYTE), 
"TVAL_CHAR" VARCHAR2(255 BYTE), 
"NVAL_NUM" NUMBER(18,5), 
"VALUEFLAG_CD" VARCHAR2(50 BYTE), 
"QUANTITY_NUM" NUMBER(18,5), 
"UNITS_CD" VARCHAR2(50 BYTE), 
"END_DATE" DATE, 
"LOCATION_CD" VARCHAR2(50 BYTE), 
"OBSERVATION_BLOB" CLOB, 
"CONFIDENCE_NUM" NUMBER(18,5), 
"UPDATE_DATE" DATE, 
"DOWNLOAD_DATE" DATE, 
"IMPORT_DATE" DATE, 
"SOURCESYSTEM_CD" VARCHAR2(50 BYTE), 
"UPLOAD_ID" NUMBER(38,0), 
"SAMPLE_CD" VARCHAR2(200 BYTE), 
 CONSTRAINT "OBSERVATION_FACT_PKEY" PRIMARY KEY ("ENCOUNTER_NUM", "PATIENT_NUM", "CONCEPT_CD", "PROVIDER_ID", "INSTANCE_NUM", "MODIFIER_CD", "START_DATE")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" 
LOB ("OBSERVATION_BLOB") STORE AS BASICFILE (
 TABLESPACE "TRANSMART" ENABLE STORAGE IN ROW CHUNK 8192 PCTVERSION 10
 NOCACHE NOLOGGING ) ;
--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: IDX_OB_FACT_2
--
CREATE INDEX "I2B2DEMODATA"."IDX_OB_FACT_2" ON "I2B2DEMODATA"."OBSERVATION_FACT" ("CONCEPT_CD", "PATIENT_NUM", "ENCOUNTER_NUM")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: IDX_OB_FACT_1
--
CREATE INDEX "I2B2DEMODATA"."IDX_OB_FACT_1" ON "I2B2DEMODATA"."OBSERVATION_FACT" ("CONCEPT_CD")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: IDX_OB_FACT_PATIENT_NUMBER
--
CREATE INDEX "I2B2DEMODATA"."IDX_OB_FACT_PATIENT_NUMBER" ON "I2B2DEMODATA"."OBSERVATION_FACT" ("PATIENT_NUM", "CONCEPT_CD")
TABLESPACE "TRANSMART" ;

--
-- Type: CONSTRAINT; Owner: I2B2DEMODATA; Name: OBS_FACT_TRIAL_VISIT_FK
--
ALTER TABLE "I2B2DEMODATA"."OBSERVATION_FACT" ADD CONSTRAINT "OBS_FACT_TRIAL_VISIT_FK"
FOREIGN KEY ("TRIAL_VISIT_NUM") REFERENCES "I2B2DEMODATA"."TRIAL_VISIT_DIMENSION" ("TRIAL_VISIT_NUM") ENABLE;

--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: IDX_OB_FACT_TRIAL_VISIT
--
CREATE INDEX "I2B2DEMODATA"."IDX_OB_FACT_TRIAL_VISIT" ON "I2B2DEMODATA"."OBSERVATION_FACT" ("TRIAL_VISIT_NUM")
TABLESPACE "INDX";

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.observation_fact IS 'Table that stores all observations';

COMMENT ON COLUMN observation_fact.encounter_num IS 'Primary key. Refers to encounter_num in visit_dimension.';
COMMENT ON COLUMN observation_fact.patient_num IS 'Primary key. Refers to patient_num in patient_dimension.';
COMMENT ON COLUMN observation_fact.concept_cd IS 'Primary key. Refers to concept_cd in concept_dimension.';
COMMENT ON COLUMN observation_fact.provider_id IS 'Primary key. Refers to provider_id in provider_dimension.';
COMMENT ON COLUMN observation_fact.start_date IS 'Primary key. Starting date-time of the observation. Default: 0001-01-01 00:00:00.';
COMMENT ON COLUMN observation_fact.end_date IS 'The end date-time of the observation';
COMMENT ON COLUMN observation_fact.modifier_cd IS 'Primary key. Refers to modifier_cd in modifier_dimension. Default: @. Highdim values: [TRANSMART:HIGHDIM:GENE EXPRESSION, TRANSMART:HIGHDIM:RNASEQ_TRANSCRIPT], original variable: [TRANSMART:ORIGINAL_VARIABLE], sample type: [TNS:SMPL].';
COMMENT ON COLUMN observation_fact.instance_num IS 'Primary key. Default: 1.';
COMMENT ON COLUMN observation_fact.trial_visit_num IS 'Refers to the new trial_visit dimension. Is not part of the primary key to make the primary key of observation_fact identical with that used by i2b2.';

COMMENT ON COLUMN observation_fact.valtype_cd IS 'Either T for string values or N for numeric values.';
COMMENT ON COLUMN observation_fact.tval_char IS 'If valtype_cd is T, the observations text value. If valtype_cd is N, an i2b2 supported operator [E = Equals, NE = Not equal, L = Less than, LE = Less than or Equal to, G = Greater than, GE = Greater than or Equal to]';
COMMENT ON COLUMN observation_fact.nval_num IS 'Used in conjunction with valtype_cd = N to store a numerical value';

COMMENT ON COLUMN observation_fact.sourcesystem_cd IS 'Deprecated. Is currently being ignored.';
COMMENT ON COLUMN observation_fact.sample_cd IS 'Deprecated. Refers to the sample_dimension table.';