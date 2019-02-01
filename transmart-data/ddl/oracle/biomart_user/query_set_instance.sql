--
-- Type: TABLE; Owner: BIOMART_USER; Name: QUERY_SET_INSTANCE
-- DEPRECATED! user queries related functionality has been moved to a gb-backend application
--
CREATE TABLE "BIOMART_USER"."QUERY_SET_INSTANCE"
(
    "ID" NUMBER NOT NULL,
    "QUERY_SET_ID" NUMBER NOT NULL,
    "OBJECT_ID" NUMBER(38,0) NOT NULL,
    PRIMARY KEY ("ID")
);

--
-- Type: SEQUENCE; Owner: BIOMART_USER; Name: QUERY_SET_INSTANCE_ID_SEQ
--
CREATE SEQUENCE "BIOMART_USER"."QUERY_SET_INSTANCE_ID_SEQ";

--
-- Type: TRIGGER; Owner: BIOMART_USER; Name: TRG_QUERY_SET_INSTANCE_ID
--
CREATE OR REPLACE TRIGGER "BIOMART_USER"."TRG_QUERY_SET_INSTANCE_ID"
before insert on "BIOMART_USER"."QUERY_SET_INSTANCE"
for each row begin
	if inserting then
		if :NEW."ID" is null then
			select QUERY_SET_INSTANCE_ID_SEQ.nextval into :NEW."ID" from dual;
		end if;
	end if;
end;

/
ALTER TRIGGER "BIOMART_USER"."TRG_QUERY_SET_INSTANCE_ID" ENABLE;

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_set_instance IS 'Table stores information about specific instances of the query set. DEPRECATED! This table has been moved to a gb-backend application database.';

COMMENT ON COLUMN biomart_user.query_set_instance.query_set_id IS 'Foreign key to id in query_set table.';
COMMENT ON COLUMN biomart_user.query_set_instance.object_id IS 'The id of the object that is being represented by the set instance, e.g. id of an i2b2demodata.patient_dimension instance.';
COMMENT ON COLUMN biomart_user.query_set_diff.change_flag IS 'The flag determining whether the object was added or removed from the related set';
