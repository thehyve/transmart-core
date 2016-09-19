--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_CREATE_EXTERNAL_TABLES
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_CREATE_EXTERNAL_TABLES" 
(
  TPMExtFn VARCHAR2,
  CATGExtFn VARCHAR2
)
AS

sqltxt varchar2(5000);

BEGIN

--  recreate CATEGORY_EXTRNL tabls with CATGExtFN parameter (filename in external file system)

sqltxt:='drop table i2b2_lz.category_extrnl';

  execute immediate sqltxt;

sqltxt:='CREATE or REPLACE  TABLE "I2B2_LZ"."CATEGORY_EXTRNL"
   ( "STUDY_ID" VARCHAR2(100 BYTE),
	"CATEGORY_CD" VARCHAR2(100 BYTE),
	"CATEGORY_PATH" VARCHAR2(250 BYTE)
   )
   ORGANIZATION EXTERNAL
    ( TYPE ORACLE_LOADER
      DEFAULT DIRECTORY "BIOMART_LZ"
      ACCESS PARAMETERS
      ( records delimited by newline nologfile skip 1
        fields terminated by 0X"09"
        MISSING FIELD VALUES ARE NULL
            )
      LOCATION
       ( ' || '''' || CATGExtFn || '''' || '))';

   execute immediate sqltxt;

--  recreate TIME_POINT_MEASUREMENT_EXTRNL tabls with TPMExtFN parameter (filename in external file system)

sqltxt:='drop table i2b2_lz.time_point_measurement_extrnl';

  execute immediate sqltxt;

sqltxt:='    CREATE or REPLACE  TABLE "I2B2_LZ"."TIME_POINT_MEASUREMENT_EXTRNL"
   ("STUDY_ID" VARCHAR2(25 BYTE),
	"USUBJID" VARCHAR2(50 BYTE),
	"SITE_ID" VARCHAR2(10 BYTE),
	"SUBJECT_ID" VARCHAR2(10 BYTE),
	"VISIT_NAME" VARCHAR2(100 BYTE),
	"DATASET_NAME" VARCHAR2(500 BYTE),
	"SAMPLE_TYPE" VARCHAR2(100 BYTE),
	"DATA_LABEL" VARCHAR2(500 BYTE),
	"DATA_VALUE" VARCHAR2(500 BYTE),
	"CATEGORY_CD" VARCHAR2(100 BYTE),
	"PERIOD" VARCHAR2(100 BYTE)
   )
   ORGANIZATION EXTERNAL
    ( TYPE ORACLE_LOADER
      DEFAULT DIRECTORY "BIOMART_LZ"
      ACCESS PARAMETERS
      ( records delimited by newline nologfile skip 1
        fields terminated by 0X"09"
        MISSING FIELD VALUES ARE NULL
            )
      LOCATION ( ' || '''' || TPMExtFn || '''' ||  ') )';

    execute immediate sqltxt;

END;


/
 
