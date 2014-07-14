--
-- Type: TABLE; Owner: AMAPP; Name: AM_TAG_VALUE
--
 CREATE TABLE "AMAPP"."AM_TAG_VALUE" 
  (	"TAG_VALUE_ID" NUMBER NOT NULL ENABLE, 
"VALUE" NVARCHAR2(2000), 
 CONSTRAINT "AM_TAG_VALUE_PK" PRIMARY KEY ("TAG_VALUE_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: TRIGGER; Owner: AMAPP; Name: TRG_AM_TAG_VALUE_ID
--
  CREATE OR REPLACE TRIGGER "AMAPP"."TRG_AM_TAG_VALUE_ID" before insert on "AMAPP"."AM_TAG_VALUE"    
for each row begin    
if inserting then      
  if :NEW."TAG_VALUE_ID" is null then          
    select SEQ_AMAPP_DATA_ID.nextval into :NEW."TAG_VALUE_ID" from dual;       
  end if;    
end if; 
end;

/
ALTER TRIGGER "AMAPP"."TRG_AM_TAG_VALUE_ID" ENABLE;
 
--
-- Type: TRIGGER; Owner: AMAPP; Name: TRG_AM_TAG_VALUE_UID
--
  CREATE OR REPLACE TRIGGER "AMAPP"."TRG_AM_TAG_VALUE_UID" after insert on "AM_TAG_VALUE"    
for each row
DECLARE
  rec_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM am_data_uid 
  WHERE am_data_id = :new.TAG_VALUE_ID;
  
  if rec_count = 0 then
    insert into amapp.am_data_uid (am_data_id, unique_id, am_data_type)
    values (:NEW."TAG_VALUE_ID", AM_TAG_VALUE_UID(:NEW."TAG_VALUE_ID"), 'AM_TAG_VALUE');
  end if;
end;

/
ALTER TRIGGER "AMAPP"."TRG_AM_TAG_VALUE_UID" ENABLE;
 
