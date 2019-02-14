--
-- Type: TABLE; Owner: I2B2METADATA; Name: DIMENSION_DESCRIPTION
--
CREATE TABLE "I2B2METADATA"."DIMENSION_DESCRIPTION"
( "ID" NUMBER,
"DENSITY" VARCHAR2(255 BYTE),
"MODIFIER_CODE" VARCHAR2(255 BYTE),
"VALUE_TYPE" VARCHAR2(50 BYTE),
"NAME" VARCHAR2(255 BYTE) NOT NULL ENABLE,
"PACKABLE" VARCHAR2(255 BYTE),
"SIZE_CD" VARCHAR2(255 BYTE),
"DIMENSION_TYPE" VARCHAR2(50 BYTE),
"SORT_INDEX" NUMBER)
NOCOMPRESS LOGGING
TABLESPACE "TRANSMART" ;

ALTER TABLE "I2B2METADATA"."DIMENSION_DESCRIPTION"
ADD CONSTRAINT "DIMENSION_DESC_UNIQUE_NAME" UNIQUE ("NAME");

ALTER TABLE "I2B2METADATA"."DIMENSION_DESCRIPTION"
ADD CONSTRAINT "DIMENSION_DESC_PK_ID" PRIMARY KEY ("ID") USING INDEX TABLESPACE "INDX" ENABLE;
--
-- Type: SEQUENCE; Owner: I2B2METADATA; Name: DIMENSION_DESCRIPTION_ID_SEQ
--
CREATE SEQUENCE  "I2B2METADATA"."DIMENSION_DESCRIPTION_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOPARTITION ;
--
-- Type: TRIGGER; Owner: I2B2METADATA; Name: DIMENSION_DESCRIPTION_ID_TRG
--
  CREATE OR REPLACE TRIGGER "I2B2METADATA"."DIMENSION_DESCRIPTION_ID_TRG"
BEFORE INSERT ON I2B2METADATA.dimension_description
FOR EACH ROW

BEGIN
  if inserting then
    if :NEW."ID" is null then
      SELECT DIMENSION_DESCRIPTION_ID_SEQ.NEXTVAL
      INTO   :new.id
      FROM   dual;
    end if;
  end if;
END;
/
ALTER TRIGGER "I2B2METADATA"."DIMENSION_DESCRIPTION_ID_TRG" ENABLE;

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.dimension_description IS 'All supported dimensions and their properties.';

COMMENT ON COLUMN dimension_description.name IS 'The name of the dimension.';
COMMENT ON COLUMN dimension_description.dimension_type IS 'Indicates whether the dimension represents subjects or observation attributes. [SUBJECT, ATTRIBUTE]';
COMMENT ON COLUMN dimension_description.sort_index IS 'Specifies a relative order between dimensions.';
COMMENT ON COLUMN dimension_description.value_type IS 'T for string, N for numeric, B for raw text and D for date values. [T, N, B, D]';
COMMENT ON COLUMN dimension_description.modifier_code IS 'The modifier code if the dimension is a modifier dimension';
COMMENT ON COLUMN dimension_description.size_cd IS 'Indicates the typical size of the dimension. [SMALL, MEDIUM, LARGE]';
COMMENT ON COLUMN dimension_description.density IS 'Indicates the typical density of the dimension. [DENSE, SPARSE]';
COMMENT ON COLUMN dimension_description.packable IS 'Indicates if dimensions values can be packed when serialising. NOT_PACKABLE is a good default. [PACKABLE, NOT_PACKABLE]';
