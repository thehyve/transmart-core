--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: CONCEPT_COUNTS
--
 CREATE TABLE "I2B2DEMODATA"."CONCEPT_COUNTS" 
  (	"CONCEPT_PATH" VARCHAR2(500 BYTE), 
"PARENT_CONCEPT_PATH" VARCHAR2(500 BYTE), 
"PATIENT_COUNT" NUMBER(38,0)
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: CONCEPT_COUNTS_INDEX1
--
CREATE INDEX "I2B2DEMODATA"."CONCEPT_COUNTS_INDEX1" ON "I2B2DEMODATA"."CONCEPT_COUNTS" ("CONCEPT_PATH")
TABLESPACE "TRANSMART" ;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.concept_counts IS 'Deprecated. Stores number of patients for which there are observations over the given concept.';

COMMENT ON COLUMN concept_counts.concept_path IS 'The concept path to which patient count is bound.';
COMMENT ON COLUMN concept_counts.parent_concept_path IS 'The concept path of the parent.';
COMMENT ON COLUMN concept_counts.patient_count IS 'The number of patients for which there are observations for the concept.';