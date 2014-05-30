--
-- Type: TABLE; Owner: TM_CZ; Name: CZ_XTRIAL_CTRL_VOCAB
--
 CREATE TABLE "TM_CZ"."CZ_XTRIAL_CTRL_VOCAB" 
  (	"CTRL_VOCAB_CODE" VARCHAR2(200 BYTE) NOT NULL ENABLE, 
"CTRL_VOCAB_NAME" VARCHAR2(200 BYTE) NOT NULL ENABLE, 
"CTRL_VOCAB_CATEGORY" VARCHAR2(200 BYTE), 
"CTRL_VOCAB_ID" NUMBER NOT NULL ENABLE
  ) SEGMENT CREATION DEFERRED
 TABLESPACE "TRANSMART" ;

--
-- Type: TRIGGER; Owner: TM_CZ; Name: TRG_CZ_XTRIAL_CTRL_VOCAB_ID
--
  CREATE OR REPLACE TRIGGER "TM_CZ"."TRG_CZ_XTRIAL_CTRL_VOCAB_ID" 
before insert on tm_cz.cz_xtrial_ctrl_vocab
for each row begin
       	if inserting then
               	if :NEW.ctrl_vocab_id is null then
                       	select seq_cz.nextval into :NEW.ctrl_vocab_id from dual;
               	end if;
       	end if;
end;
/
ALTER TRIGGER "TM_CZ"."TRG_CZ_XTRIAL_CTRL_VOCAB_ID" ENABLE;
 
