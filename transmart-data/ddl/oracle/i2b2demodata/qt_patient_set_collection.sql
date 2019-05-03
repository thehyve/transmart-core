--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: QT_PATIENT_SET_COLLECTION
--
 CREATE TABLE "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION" 
  (	"PATIENT_SET_COLL_ID" NUMBER(10,0) NOT NULL ENABLE, 
"RESULT_INSTANCE_ID" NUMBER(5,0), 
"SET_INDEX" NUMBER(10,0), 
"PATIENT_NUM" NUMBER(10,0), 
 CONSTRAINT "QT_PATIENT_SET_COLLECTION_PKEY" PRIMARY KEY ("PATIENT_SET_COLL_ID")
 USING INDEX
 TABLESPACE "TRANSMART"  ENABLE
  ) SEGMENT CREATION IMMEDIATE
 TABLESPACE "TRANSMART" ;

--
-- Type: REF_CONSTRAINT; Owner: I2B2DEMODATA; Name: QT_FK_PSC_RI
--
ALTER TABLE "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION" ADD CONSTRAINT "QT_FK_PSC_RI" FOREIGN KEY ("RESULT_INSTANCE_ID")
 REFERENCES "I2B2DEMODATA"."QT_QUERY_RESULT_INSTANCE" ("RESULT_INSTANCE_ID") ENABLE;

--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: QT_SQ_QPR_PCID
--
CREATE SEQUENCE  "I2B2DEMODATA"."QT_SQ_QPR_PCID"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 4430157 CACHE 20 NOORDER  NOCYCLE ;

--
-- Type: TRIGGER; Owner: I2B2DEMODATA; Name: TR_QT_PSC_PSC_ID
--
  CREATE OR REPLACE TRIGGER "I2B2DEMODATA"."TR_QT_PSC_PSC_ID" 
   before insert on "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION" 
   for each row 
begin  
   if inserting then 
      if :NEW."PATIENT_SET_COLL_ID" is null then 
         select QT_SQ_QPR_PCID.nextval into :NEW."PATIENT_SET_COLL_ID" from dual; 
      end if; 
   end if; 
end;
/
ALTER TRIGGER "I2B2DEMODATA"."TR_QT_PSC_PSC_ID" ENABLE;
 
--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: QT_IDX_QPSC_RIID
--
CREATE INDEX "I2B2DEMODATA"."QT_IDX_QPSC_RIID" ON "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION" ("RESULT_INSTANCE_ID")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: I2B2DEMODATA; Name: QT_IDX_QPSC_RIID_PN
--
CREATE INDEX "I2B2DEMODATA"."QT_IDX_QPSC_RIID_PN" ON "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION" ("RESULT_INSTANCE_ID", "PATIENT_NUM")
TABLESPACE "TRANSMART" ;
