--
-- Type: TABLE; Owner: I2B2METADATA; Name: I2B2_SECURE
--
 CREATE TABLE "I2B2METADATA"."I2B2_SECURE" 
  (	"C_HLEVEL" NUMBER(22,0), 
"C_FULLNAME" VARCHAR2(900 BYTE) NOT NULL ENABLE, 
"C_NAME" VARCHAR2(2000 BYTE), 
"C_SYNONYM_CD" CHAR(1 BYTE), 
"C_VISUALATTRIBUTES" CHAR(3 BYTE), 
"C_TOTALNUM" NUMBER(22,0), 
"C_BASECODE" VARCHAR2(450 BYTE), 
"C_METADATAXML" CLOB, 
"C_FACTTABLECOLUMN" VARCHAR2(50 BYTE), 
"C_TABLENAME" VARCHAR2(150 BYTE), 
"C_COLUMNNAME" VARCHAR2(50 BYTE), 
"C_COLUMNDATATYPE" VARCHAR2(50 BYTE), 
"C_OPERATOR" VARCHAR2(10 BYTE), 
"C_DIMCODE" VARCHAR2(900 BYTE), 
"C_COMMENT" CLOB, 
"C_TOOLTIP" VARCHAR2(900 BYTE), 
"M_APPLIED_PATH" VARCHAR2(700 BYTE) DEFAULT ('@'), 
"UPDATE_DATE" TIMESTAMP (6), 
"DOWNLOAD_DATE" TIMESTAMP (6), 
"IMPORT_DATE" TIMESTAMP (6), 
"SOURCESYSTEM_CD" VARCHAR2(50 BYTE), 
"VALUETYPE_CD" VARCHAR2(50 BYTE), 
"M_EXCLUSION_CD" VARCHAR2(25 BYTE), 
"C_SYMBOL" VARCHAR2(50 BYTE), 
"C_PATH" VARCHAR2(900 BYTE), 
"I2B2_ID" NUMBER(18,0), 
"SECURE_OBJ_TOKEN" VARCHAR2(50 BYTE)
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" 
LOB ("C_METADATAXML") STORE AS BASICFILE (
 TABLESPACE "TRANSMART" ENABLE STORAGE IN ROW CHUNK 8192 PCTVERSION 10
 NOCACHE NOLOGGING ) 
LOB ("C_COMMENT") STORE AS BASICFILE (
 TABLESPACE "TRANSMART" ENABLE STORAGE IN ROW CHUNK 8192 PCTVERSION 10
 NOCACHE NOLOGGING ) ;

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.i2b2_secure IS 'The same as i2b2, but with an added security token.';

COMMENT ON COLUMN i2b2_secure.c_hlevel IS 'Number that represents the depth of the node. 0 for root.';
COMMENT ON COLUMN i2b2_secure.c_fullname IS 'Full path to the node. E.g., \Vital Signs\Heart Rate\.';
COMMENT ON COLUMN i2b2_secure.c_name IS 'Name of the node. E.g., Heart Rate.';
COMMENT ON COLUMN i2b2_secure.c_basecode IS 'Code that represents node. E.g., VSIGN:HR. Not used.';
COMMENT ON COLUMN i2b2_secure.c_visualattributes IS 'Visual attributes describing how a node should be displayed. Can have three characters at maximum. See OntologyTerm#VisualAttributes for documentation on the values.';
COMMENT ON COLUMN i2b2_secure.c_metadataxml IS 'Metadata encoded as XML.';
COMMENT ON COLUMN i2b2_secure.c_facttablecolumn IS 'Column of observation_fact corresponding with c_columnname.';
COMMENT ON COLUMN i2b2_secure.c_tablename IS 'Table of the dimension referred to by this node.';
COMMENT ON COLUMN i2b2_secure.c_columnname IS 'Column of the table of the dimension referred to by this node';
COMMENT ON COLUMN i2b2_secure.c_operator IS 'Operator. E.g., like, =';
COMMENT ON COLUMN i2b2_secure.c_dimcode IS 'Refers to a dimension element, linked to observations.';
COMMENT ON COLUMN i2b2_secure.c_comment IS 'Meant for comments, not for storing study based security tokens.';

COMMENT ON COLUMN i2b2_secure.secure_obj_token IS 'Token encoding access the node. Refers to bio_data_unique_id in searchapp.search_secure_object. E.g., PUBLIC or EXP:SCSCP.';
