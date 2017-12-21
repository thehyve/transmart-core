--
-- Type: TABLE; Owner: BIOMART_USER; Name: QUERY_DIFF
--
CREATE TABLE "BIOMART_USER"."QUERY_DIFF"
(
    "ID" NUMBER NOT NULL ENABLE,
    "QUERY_ID" NUMBER NOT NULL,
    "SET_ID" NUMBER(38,0) NOT NULL,
    "SET_TYPE" VARCHAR2(25 BYTE) NOT NULL,
    "DATE" TIMESTAMP (6) NOT NULL,
    PRIMARY KEY ("ID")
);

--
-- Type: SEQUENCE; Owner: BIOMART_USER; Name: QUERY_DIFF_ID_SEQ
--
CREATE SEQUENCE "BIOMART_USER"."QUERY_DIFF_ID_SEQ";

--
-- Type: TRIGGER; Owner: BIOMART_USER; Name: TRG_QUERY_DIFF_ID
--
  CREATE OR REPLACE TRIGGER "BIOMART_USER"."TRG_QUERY_DIFF_ID"
	 before insert on "QUERY_DIFF"
	 for each row begin
	 if inserting then
	 if :NEW."ID" is null then
	 select QUERY_DIFF_ID_SEQ.nextval into :NEW."ID" from dual;
	 end if;
	 end if;
	 end;

/
ALTER TRIGGER "BIOMART_USER"."TRG_QUERY_DIFF_ID" ENABLE;

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_diff IS 'Table stores information about data changes for subscribed user queries.';

COMMENT ON COLUMN biomart_user.query_diff.query_id IS 'Foreign key to id in query table.';
COMMENT ON COLUMN biomart_user.query_diff.set_id IS 'The id of the set of objects that the data change relates to';
COMMENT ON COLUMN biomart_user.query_diff.set_type IS 'The type of set: [Patient | Sample].';
COMMENT ON COLUMN biomart_user.query_diff.date IS 'The date of the data change detection.';
