--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: STORAGE_SYSTEM
--
 CREATE TABLE "I2B2DEMODATA"."STORAGE_SYSTEM"
  (	"ID" NUMBER(*,0) NOT NULL ENABLE,
"NAME" VARCHAR2(50 BYTE),
"SYSTEM_TYPE" VARCHAR2(50 BYTE),
"URL" VARCHAR2(900 BYTE),
"SYSTEM_VERSION" VARCHAR2(50 BYTE),
"SINGLE_FILE_COLLECTIONS" CHAR(1 BYTE),
 CONSTRAINT "STORAGE_SYSTEM_PKEY" PRIMARY KEY ("ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
NOCOMPRESS LOGGING
 TABLESPACE "TRANSMART" ;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.storage_system IS 'Registered storage instances.';

COMMENT ON COLUMN storage_system.name IS 'Name of the system.';
COMMENT ON COLUMN storage_system.system_type IS 'Storage system type. e.g. Arvados';
COMMENT ON COLUMN storage_system.url IS 'URL of the instance.';
COMMENT ON COLUMN storage_system.system_version IS 'Version of the storage system, used for formulating requests by the frontend.';
COMMENT ON COLUMN storage_system.single_file_collections IS 'True for systems where collection=one file, false for those where collection=file tree (like Arvados Keep)';