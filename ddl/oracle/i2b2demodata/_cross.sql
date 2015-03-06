--
-- Type: TRIGGER; Owner: I2B2DEMODATA; Name: TRG_CONCEPT_DIMENSION_CD
--
  CREATE OR REPLACE TRIGGER "I2B2DEMODATA"."TRG_CONCEPT_DIMENSION_CD" 
	 before insert on "CONCEPT_DIMENSION"
	 for each row begin
	 if inserting then
	 if :NEW."CONCEPT_CD" is null then
	 select CONCEPT_ID.nextval into :NEW."CONCEPT_CD" from dual;
	 end if;
	 end if;
	 end;

/
ALTER TRIGGER "I2B2DEMODATA"."TRG_CONCEPT_DIMENSION_CD" ENABLE;
 
