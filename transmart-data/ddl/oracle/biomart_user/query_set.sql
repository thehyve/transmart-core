--
-- Type: TABLE; Owner: BIOMART_USER; Name: QUERY_SET
--
CREATE TABLE "BIOMART_USER"."QUERY_SET"
(
    "ID" NUMBER NOT NULL ENABLE,
    "QUERY_ID" NUMBER NOT NULL,
    "SET_SIZE" NUMBER(38,0) NOT NULL,
    "SET_TYPE" VARCHAR2(25 BYTE) NOT NULL,
    "CREATE_DATE" TIMESTAMP (6) NOT NULL,
    PRIMARY KEY ("ID")
);

--
-- Type: SEQUENCE; Owner: BIOMART_USER; Name: QUERY_SET_ID_SEQ
--
CREATE SEQUENCE "BIOMART_USER"."QUERY_SET_ID_SEQ";

--
-- Type: TRIGGER; Owner: BIOMART_USER; Name: TRG_QUERY_SET_ID
--
CREATE OR REPLACE TRIGGER "BIOMART_USER"."TRG_QUERY_SET_ID"
	 before insert on "BIOMART_USER"."QUERY_SET"
	 for each row begin
	 if inserting then
	 if :NEW."ID" is null then
	 select QUERY_SET_ID_SEQ.nextval into :NEW."ID" from dual;
	 end if;
	 end if;
	 end;

/
ALTER TRIGGER "BIOMART_USER"."TRG_QUERY_SET_ID" ENABLE;

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_set IS 'Table stores information about data sets for user queries.';

COMMENT ON COLUMN biomart_user.query_set.query_id IS 'Foreign key to id in a query table.';
COMMENT ON COLUMN biomart_user.query_set.set_size IS 'The size of the set';
COMMENT ON COLUMN biomart_user.query_set.set_type IS 'The type of the set: [Patient | Sample].';
COMMENT ON COLUMN biomart_user.query_set.create_date IS 'The date of the set creation.';
