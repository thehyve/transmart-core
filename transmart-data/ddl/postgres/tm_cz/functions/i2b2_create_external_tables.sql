--
-- Name: i2b2_create_external_tables(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_create_external_tables(tpmextfn character varying, catgextfn character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE


sqltxt varchar(5000);


BEGIN

--  recreate CATEGORY_EXTRNL tabls with CATGExtFN parameter (filename in external file system)

sqltxt:='drop table i2b2_lz.category_extrnl';

  EXECUTE sqltxt;

sqltxt:='CREATE or REPLACE  TABLE "I2B2_LZ"."CATEGORY_EXTRNL"
   ( "STUDY_ID" varchar(100 BYTE),
	"CATEGORY_CD" varchar(100 BYTE),
	"CATEGORY_PATH" varchar(250 BYTE)
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

   EXECUTE sqltxt;

--  recreate TIME_POINT_MEASUREMENT_EXTRNL tabls with TPMExtFN parameter (filename in external file system)

sqltxt:='drop table i2b2_lz.time_point_measurement_extrnl';

  EXECUTE sqltxt;

sqltxt:='    CREATE or REPLACE  TABLE "I2B2_LZ"."TIME_POINT_MEASUREMENT_EXTRNL"
   ("STUDY_ID" varchar(25 BYTE),
	"USUBJID" varchar(50 BYTE),
	"SITE_ID" varchar(10 BYTE),
	"SUBJECT_ID" varchar(10 BYTE),
	"VISIT_NAME" varchar(100 BYTE),
	"DATASET_NAME" varchar(500 BYTE),
	"SAMPLE_TYPE" varchar(100 BYTE),
	"DATA_LABEL" varchar(500 BYTE),
	"DATA_VALUE" varchar(500 BYTE),
	"CATEGORY_CD" varchar(100 BYTE),
	"PERIOD" varchar(100 BYTE)
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

    EXECUTE sqltxt;

END;

$$;

