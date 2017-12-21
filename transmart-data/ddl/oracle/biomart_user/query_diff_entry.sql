--
-- Type: TABLE; Owner: BIOMART_USER; Name: QUERY_DIFF_ENTRY
--
CREATE TABLE "BIOMART_USER"."QUERY_DIFF_ENTRY"
(
    "ID" NUMBER NOT NULL,
    "QUERY_DIFF_ID" NUMBER NOT NULL,
    "OBJECT_ID" NUMBER(38,0) NOT NULL,
    "CHANGE_FLAG" character varying(7) NOT NULL,
    PRIMARY KEY ("ID")
);

--
-- Type: SEQUENCE; Owner: BIOMART_USER; Name: QUERY_DIFF_ENTRY_ID_SEQ
--
CREATE SEQUENCE "BIOMART_USER"."QUERY_DIFF_ENTRY_ID_SEQ";

--
-- Type: TRIGGER; Owner: BIOMART_USER; Name: TRG_QUERY_DIFF_ENTRY_ID
--
CREATE OR REPLACE TRIGGER "BIOMART_USER"."TRG_QUERY_DIFF_ENTRY_ID"
before insert on "QUERY_DIFF_ENTRY"
for each row begin
	if inserting then
		if :NEW."ID" is null then
			select QUERY_DIFF_ENTRY_ID_SEQ.nextval into :NEW."ID" from dual;
		end if;
	end if;
end;

/
ALTER TRIGGER "BIOMART_USER"."TRG_QUERY_DIFF_ID" ENABLE;

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_diff_entry IS 'Table stores information about specific objects deleted or added  data changes for subscribed user queries.';

COMMENT ON COLUMN biomart_user.query_diff_entry.query_diff_id IS 'Foreign key to id in query_diff table.';
COMMENT ON COLUMN biomart_user.query_diff_entry.object_id IS 'The id of the object from the set that was updated.';
COMMENT ON COLUMN biomart_user.query_diff_entry.change_flag IS 'The flag determining whether the object was added or removed from the related set';
