--
-- Type: TABLE; Owner: BIOMART; Name: BIO_ANALYSIS_ATTRIBUTE_LINEAGE
--
 CREATE TABLE "BIOMART"."BIO_ANALYSIS_ATTRIBUTE_LINEAGE" 
  (	"BIO_ANALYSIS_ATT_LINEAGE_ID" NUMBER NOT NULL ENABLE, 
"BIO_ANALYSIS_ATTRIBUTE_ID" NUMBER NOT NULL ENABLE, 
"ANCESTOR_TERM_ID" NUMBER NOT NULL ENABLE, 
"ANCESTOR_SEARCH_KEYWORD_ID" NUMBER NOT NULL ENABLE, 
 CONSTRAINT "PK_BAAL_ID" PRIMARY KEY ("BIO_ANALYSIS_ATT_LINEAGE_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION DEFERRED
 TABLESPACE "TRANSMART" ;

--
-- Type: TRIGGER; Owner: BIOMART; Name: TRG_BIO_ANALYSIS_ATT_LINEAGE
--
  CREATE OR REPLACE TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATT_LINEAGE" 
  before insert on "BIOMART"."BIO_ANALYSIS_ATTRIBUTE_LINEAGE"
  for each row
declare
  -- local variables here
begin
  if inserting then
    if :NEW.BIO_ANALYSIS_ATT_LINEAGE_ID is null then
      select SEQ_BIO_ANALYSIS_ATT_LIN_ID.nextval into :NEW.BIO_ANALYSIS_ATT_LINEAGE_ID from dual;
    end if;
  end if;
end TRG_BIO_ANALYSIS_ATT_LINEAGE;


/
ALTER TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATT_LINEAGE" ENABLE;
 
--
-- Type: TRIGGER; Owner: BIOMART; Name: TRG_BIO_ANALYSIS_AL_ID
--
  CREATE OR REPLACE TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_AL_ID" 
before insert on biomart.bio_analysis_attribute_lineage
for each row begin
       	if inserting then
               	if :NEW.bio_analysis_att_lineage_id is null then
                       	select seq_bio_data_id.nextval into :NEW.bio_analysis_att_lineage_id from dual;
               	end if;
       	end if;
end;
/
ALTER TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_AL_ID" ENABLE;
 
