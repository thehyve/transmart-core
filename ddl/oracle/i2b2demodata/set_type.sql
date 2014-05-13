--
-- Type: TABLE; Owner: I2B2DEMODATA; Name: SET_TYPE
--
 CREATE TABLE "I2B2DEMODATA"."SET_TYPE" 
  (	"ID" NUMBER(38,0), 
"NAME" VARCHAR2(500 BYTE), 
"CREATE_DATE" DATE,
CONSTRAINT "PK_ST_ID" PRIMARY KEY ("ID") --postgres
  ) SEGMENT CREATION DEFERRED
NOCOMPRESS NOLOGGING
 TABLESPACE "I2B2_DATA" ;

-- trigger needed to match postgres default values

-- Type: TRIGGER; Owner: I2B2DEMODATA; Name: TRG_SET_TYPE_ID
--
  CREATE OR REPLACE TRIGGER "I2B2DEMODATA"."TRG_SET_TYPE_ID" before insert on "I2B2DEMODATA"."SET_TYPE"    
for each row begin    
if inserting then      
  if :NEW."ID" is null then          
    select SQ_UP_PATDIM_PATIENTNUM.nextval into :NEW."ID" from dual;       
  end if;    
end if; 
end;
/
