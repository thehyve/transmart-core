--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: RELATION_TYPE
--
CREATE TABLE "I2B2DEMODATA"."RELATION_TYPE" (
    "ID" NUMBER NOT NULL,
    "LABEL" VARCHAR2(200 BYTE) NOT NULL,
    "DESCRIPTION" CLOB,
    "SYMMETRICAL" CHAR(1 BYTE),
    "BIOLOGICAL" CHAR(1 BYTE),
    PRIMARY KEY ("ID"),
    CONSTRAINT "RELATION_TYPE_LABEL_UNQ" UNIQUE ("LABEL")
);

--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: RELATION_TYPE_ID_SEQ
--
CREATE SEQUENCE "I2B2DEMODATA"."RELATION_TYPE_ID_SEQ";

--
-- Type: TRIGGER; Owner: I2B2DEMODATA; Name: TRG_RELATION_TYPE_ID
--
  CREATE OR REPLACE TRIGGER "I2B2DEMODATA"."TRG_RELATION_TYPE_ID"
	 before insert on "RELATION_TYPE"
	 for each row begin
	 if inserting then
	 if :NEW."ID" is null then
	 select RELATION_TYPE_ID_SEQ.nextval into :NEW."ID" from dual;
	 end if;
	 end if;
	 end;

/
ALTER TRIGGER "I2B2DEMODATA"."TRG_RELATION_TYPE_ID" ENABLE;

COMMENT ON TABLE i2b2demodata.relation_type IS 'Dictionary of relations. e.g. "parent of" relation.';

COMMENT ON COLUMN i2b2demodata.relation_type.label IS 'Short unique name of the relation.';
COMMENT ON COLUMN i2b2demodata.relation_type.description IS 'Detailed description of the relation.';
COMMENT ON COLUMN i2b2demodata.relation_type.symmetrical IS 'Whether relation is symmetrical. e.g. "sibling of" is symmetrical. "parent of" is not.';
COMMENT ON COLUMN i2b2demodata.relation_type.biological IS 'Whether relation is biological. e.g. "parent of" is biological. "spouse of" is not.';