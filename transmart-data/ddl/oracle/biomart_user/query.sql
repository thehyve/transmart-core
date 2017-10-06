--
-- Type: TABLE; Owner: BIOMART_USER; Name: QUERY
--
CREATE TABLE "BIOMART_USER"."QUERY"
(
    "ID" NUMBER NOT NULL ENABLE,
    "NAME" VARCHAR2(1000 BYTE) NOT NULL,
    "USERNAME" VARCHAR2(50 BYTE) NOT NULL,
    "PATIENTS_QUERY" CLOB,
    "OBSERVATIONS_QUERY" CLOB,
    "API_VERSION" VARCHAR2(25 BYTE),
    "BOOKMARKED" CHAR(1 BYTE),
    "DELETED" CHAR(1 BYTE),
    "CREATE_DATE" TIMESTAMP (6),
    "UPDATE_DATE" TIMESTAMP (6),
    PRIMARY KEY ("ID")
);

--
-- Type: INDEX; Owner: BIOMART_USERNAME_DELETED; Name: QUERY_USER
--
CREATE INDEX "BIOMART_USER"."QUERY_USERNAME_DELETED" ON "BIOMART_USER"."QUERY" ("USERNAME", "DELETED")
TABLESPACE "TRANSMART" ;

--
-- Type: SEQUENCE; Owner: BIOMART_USER; Name: QUERY_ID_SEQ
--
CREATE SEQUENCE "BIOMART_USER"."QUERY_ID_SEQ";

--
-- Type: TRIGGER; Owner: BIOMART_USER; Name: TRG_QUERY_ID
--
  CREATE OR REPLACE TRIGGER "BIOMART_USER"."TRG_QUERY_ID"
	 before insert on "QUERY"
	 for each row begin
	 if inserting then
	 if :NEW."ID" is null then
	 select QUERY_ID_SEQ.nextval into :NEW."ID" from dual;
	 end if;
	 end if;
	 end;

/
ALTER TRIGGER "BIOMART_USER"."TRG_QUERY_ID" ENABLE;

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query IS 'Storage for patients and observations queries to support front end functionality.';

COMMENT ON COLUMN biomart_user.query.name IS 'The query name.';
COMMENT ON COLUMN biomart_user.query.username IS 'The username of the user that created the query.';
COMMENT ON COLUMN biomart_user.query.patients_query IS 'The patient selection part of the query.';
COMMENT ON COLUMN biomart_user.query.observations_query IS 'The observation selection part of the query.';
COMMENT ON COLUMN biomart_user.query.api_version IS 'The version of the API the query was intended for.';
COMMENT ON COLUMN biomart_user.query.bookmarked IS 'Flag to indicate if the user has bookmarked the query.';
COMMENT ON COLUMN biomart_user.query.deleted IS 'Flag to indicate if the query has been deleted.';
