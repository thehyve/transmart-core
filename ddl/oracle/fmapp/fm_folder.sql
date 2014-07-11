--
-- Type: TABLE; Owner: FMAPP; Name: FM_FOLDER
--
 CREATE TABLE "FMAPP"."FM_FOLDER" 
  (	"FOLDER_ID" NUMBER(18,0) NOT NULL ENABLE, 
"FOLDER_NAME" NVARCHAR2(1000) NOT NULL ENABLE, 
"FOLDER_FULL_NAME" NVARCHAR2(1000) NOT NULL ENABLE, 
"FOLDER_LEVEL" NUMBER(18,0) NOT NULL ENABLE, 
"FOLDER_TYPE" NVARCHAR2(100) NOT NULL ENABLE, 
"FOLDER_TAG" NVARCHAR2(50), 
"ACTIVE_IND" CHAR(1 BYTE) NOT NULL ENABLE, 
"PARENT_ID" NUMBER(18,0), 
"DESCRIPTION" NVARCHAR2(2000), 
 PRIMARY KEY ("FOLDER_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: TRIGGER; Owner: FMAPP; Name: TRG_FM_FOLDER_ID
--
  CREATE OR REPLACE TRIGGER "FMAPP"."TRG_FM_FOLDER_ID" before insert on FM_FOLDER    
for each row begin    
if inserting then      
  if :NEW.FOLDER_ID is null then          
    select SEQ_FM_ID.nextval into :NEW.FOLDER_ID from dual;       
  end if;
  if :new.FOLDER_FULL_NAME is null then
    if :new.PARENT_ID is null then
      select '\' || fm_folder_uid(:new.folder_id) || '\' into :new.folder_full_name 
      from dual;
    else
      select folder_full_name || fm_folder_uid(:new.folder_id) || '\' into :new.folder_full_name 
      from fm_folder
      where folder_id = :new.parent_id;
    end if;
  end if;
end if; 
end;
/
ALTER TRIGGER "FMAPP"."TRG_FM_FOLDER_ID" ENABLE;
 
--
-- Type: TRIGGER; Owner: FMAPP; Name: TRG_FM_FOLDER_UID
--
  CREATE OR REPLACE TRIGGER "FMAPP"."TRG_FM_FOLDER_UID" after insert on "FM_FOLDER"    
for each row
DECLARE
  rec_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM fm_data_uid 
  WHERE fm_data_id = :new.FOLDER_ID;
  
  if rec_count = 0 then
    insert into fmapp.fm_data_uid (fm_data_id, unique_id, fm_data_type)
    values (:NEW."FOLDER_ID", FM_FOLDER_UID(:NEW."FOLDER_ID"), 'FM_FOLDER');
  end if;
end;
/
ALTER TRIGGER "FMAPP"."TRG_FM_FOLDER_UID" ENABLE;
 
