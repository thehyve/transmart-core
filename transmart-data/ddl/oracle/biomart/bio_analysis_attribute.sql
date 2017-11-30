--
-- Type: TABLE; Owner: BIOMART; Name: BIO_ANALYSIS_ATTRIBUTE
--
 CREATE TABLE "BIOMART"."BIO_ANALYSIS_ATTRIBUTE" 
  (	"STUDY_ID" VARCHAR2(255 BYTE), 
"BIO_ASSAY_ANALYSIS_ID" NUMBER NOT NULL ENABLE, 
"TERM_ID" NUMBER, 
"SOURCE_CD" VARCHAR2(255 BYTE), 
"BIO_ANALYSIS_ATTRIBUTE_ID" NUMBER NOT NULL ENABLE, 
 CONSTRAINT "PK_BAA_ID" PRIMARY KEY ("BIO_ANALYSIS_ATTRIBUTE_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: TRIGGER; Owner: BIOMART; Name: TRG_BIO_ANALYSIS_ATTRIBUTE_ID
--
  CREATE OR REPLACE TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATTRIBUTE_ID" 
before insert on biomart.bio_analysis_attribute
for each row begin
       	if inserting then
               	if :NEW.bio_analysis_attribute_id is null then
                       	select seq_bio_data_id.nextval into :NEW.bio_analysis_attribute_id from dual;
               	end if;
       	end if;
end;
/
ALTER TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATTRIBUTE_ID" ENABLE;
 
