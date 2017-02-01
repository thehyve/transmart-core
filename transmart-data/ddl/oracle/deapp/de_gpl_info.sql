--
-- Type: TABLE; Owner: DEAPP; Name: DE_GPL_INFO
--
 CREATE TABLE "DEAPP"."DE_GPL_INFO" 
  (	"PLATFORM" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
"TITLE" VARCHAR2(500 BYTE), 
"ORGANISM" VARCHAR2(100 BYTE), 
"ANNOTATION_DATE" TIMESTAMP (6), 
"MARKER_TYPE" VARCHAR2(100 BYTE), 
"RELEASE_NBR" VARCHAR2(50 BYTE), 
"GENOME_BUILD" VARCHAR2(20 BYTE), 
"GENE_ANNOTATION_ID" VARCHAR2(50 BYTE), 
 CONSTRAINT "DE_GPL_INFO_PKEY" PRIMARY KEY ("PLATFORM")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- add documentation
--
COMMENT ON TABLE deapp.de_gpl_info IS 'Definition of GPL platforms';

COMMENT ON COLUMN de_gpl_info.platform IS 'Primary key. Platform id. E.g., GPL1000, GPL96, RNASEQ_TRANSCRIPT_PLATFORM.';
COMMENT ON COLUMN de_gpl_info.title IS 'Title of the platform. E.g., microarray test data, rnaseq transcript level test data.';
COMMENT ON COLUMN de_gpl_info.organism IS 'Organism the platform applies to. E.g., Human.';
COMMENT ON COLUMN de_gpl_info.marker_type IS 'E.g., Gene Expression, RNASEQ_TRANSCRIPT.';
COMMENT ON COLUMN de_gpl_info.genome_build IS 'E.g., hg19.';