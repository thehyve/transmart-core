--
-- Type: TABLE; Owner: DEAPP; Name: DE_RNASEQ_TRANSCRIPT_DATA
--
 CREATE TABLE "DEAPP"."DE_RNASEQ_TRANSCRIPT_DATA"
  (	"TRANSCRIPT_ID" NUMBER NOT NULL ENABLE,
"ASSAY_ID" NUMBER NOT NULL ENABLE,
"READCOUNT" NUMBER,
"NORMALIZED_READCOUNT" FLOAT(126),
"LOG_NORMALIZED_READCOUNT" FLOAT(126),
"ZSCORE" FLOAT(126),
 CONSTRAINT "DE_RNASEQ_TRANSCRIPT_DATA" PRIMARY KEY ("ASSAY_ID", "TRANSCRIPT_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
NOCOMPRESS LOGGING
 TABLESPACE "TRANSMART" ;
--
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: DE_RNASEQ_TR_DATA_TR_ID_FKEY
--
ALTER TABLE "DEAPP"."DE_RNASEQ_TRANSCRIPT_DATA" ADD CONSTRAINT "DE_RNASEQ_TR_DATA_TR_ID_FKEY" FOREIGN KEY ("TRANSCRIPT_ID")
 REFERENCES "DEAPP"."DE_RNASEQ_TRANSCRIPT_ANNOT" ("ID") ENABLE;


--
-- add documentation
--
COMMENT ON TABLE deapp.de_rnaseq_transcript_data IS 'Table holds rnaseq transcript level values.';

COMMENT ON COLUMN de_rnaseq_transcript_data.transcript_id IS 'Id of the transcript, linking to id in de_rnaseq_transcript_annot.';
COMMENT ON COLUMN de_rnaseq_transcript_data.assay_id IS 'Primary key. Id used to link highdim data to assays in the de_subject_sample_mapping table.';
COMMENT ON COLUMN de_rnaseq_transcript_data.readcount IS 'Base count.';
COMMENT ON COLUMN de_rnaseq_transcript_data.normalized_readcount IS 'Normalized projection.';
COMMENT ON COLUMN de_rnaseq_transcript_data.log_normalized_readcount IS 'Log projection.';
COMMENT ON COLUMN de_rnaseq_transcript_data.zscore IS 'Zscore projection.';