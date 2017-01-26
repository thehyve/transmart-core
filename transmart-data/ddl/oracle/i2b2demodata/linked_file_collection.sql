--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: LINKED_FILE_COLLECTION
--
 CREATE TABLE "I2B2DEMODATA"."LINKED_FILE_COLLECTION"
  (	"ID" NUMBER(*,0) NOT NULL ENABLE,
"NAME" VARCHAR2(900 BYTE),
"STUDY_ID" NUMBER(*,0) NOT NULL ENABLE,
"SOURCE_SYSTEM_ID" NUMBER(*,0) NOT NULL ENABLE,
"UUID" VARCHAR2(50 BYTE),
 CONSTRAINT "LINKED_FILE_COLLECTION_PKEY" PRIMARY KEY ("ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
NOCOMPRESS LOGGING
 TABLESPACE "TRANSMART" ;
--
-- Type: REF_CONSTRAINT; Owner: I2B2DEMODATA; Name: STUDY_ID_FK
--
ALTER TABLE "I2B2DEMODATA"."LINKED_FILE_COLLECTION" ADD CONSTRAINT "STUDY_ID_FK" FOREIGN KEY ("STUDY_ID")
 REFERENCES "I2B2DEMODATA"."STUDY" ("STUDY_NUM") ENABLE;
--
-- Type: REF_CONSTRAINT; Owner: I2B2DEMODATA; Name: SOURCE_SYSTEM_ID_ID_FK
--
ALTER TABLE "I2B2DEMODATA"."LINKED_FILE_COLLECTION" ADD CONSTRAINT "SOURCE_SYSTEM_ID_ID_FK" FOREIGN KEY ("SOURCE_SYSTEM_ID")
 REFERENCES "I2B2DEMODATA"."STORAGE_SYSTEM" ("ID") ENABLE;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.linked_file_collection IS 'Stores universally unique identifier to the collection of files (a folder) on an external file system (e.g. Arvados Keep)';

COMMENT ON COLUMN linked_file_collection.name IS 'Name of the collection.';
COMMENT ON COLUMN linked_file_collection.study_id IS 'Points to the study the file associated with.';
COMMENT ON COLUMN linked_file_collection.source_system_id IS 'Foreign key. Specifies storage instance (e.g. a Arvados Keep server).';
COMMENT ON COLUMN linked_file_collection.source_system_id IS 'The universally unique identifier on the storage system.';