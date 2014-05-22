--------------------------------------------------------
--  DDL for Table SEARCH_TAXONOMY
--------------------------------------------------------

  CREATE TABLE "SEARCHAPP"."SEARCH_TAXONOMY" 
   (	"TERM_ID" NUMBER(22,0), 
	"TERM_NAME" VARCHAR2(900 BYTE), 
	"SOURCE_CD" VARCHAR2(900 BYTE), 
	"IMPORT_DATE" TIMESTAMP (1) DEFAULT Sysdate, 
	"SEARCH_KEYWORD_ID" NUMBER(38,0)
   )
  TABLESPACE "BIOMART" ;
  
--------------------------------------------------------
--  DDL for Index SEARCH_TAXONOMY_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "SEARCHAPP"."SEARCH_TAXONOMY_PK" ON "SEARCHAPP"."SEARCH_TAXONOMY" ("TERM_ID") 

--------------------------------------------------------
--  Constraints for Table SEARCH_TAXONOMY
--------------------------------------------------------

  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY" ADD CONSTRAINT "SEARCH_TAXONOMY_PK" PRIMARY KEY ("TERM_ID")
  TABLESPACE "BIOMART"  ENABLE;
 
  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY" MODIFY ("TERM_ID" NOT NULL ENABLE);
 
  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY" MODIFY ("TERM_NAME" NOT NULL ENABLE);

--------------------------------------------------------
--  Ref Constraints for Table SEARCH_TAXONOMY
--------------------------------------------------------

  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY" ADD CONSTRAINT "FK_SEARCH_TAX_SEARCH_KEYWORD" FOREIGN KEY ("SEARCH_KEYWORD_ID")
	  REFERENCES "SEARCHAPP"."SEARCH_KEYWORD" ("SEARCH_KEYWORD_ID") ENABLE;

--------------------------------------------------------
--  DDL for Trigger TGR_SEARCH_TAXONOMY_TERM_ID
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "SEARCHAPP"."TGR_SEARCH_TAXONOMY_TERM_ID" 
  before insert on "SEARCHAPP"."SEARCH_TAXONOMY" for each row
begin 
    If Inserting 
      Then If :New.Term_Id Is Null 
        Then Select SEQ_SEARCH_DATA_ID.nextval Into :New.Term_Id From Dual; 
      End If; 
    end if;
end;


/
ALTER TRIGGER "SEARCHAPP"."TGR_SEARCH_TAXONOMY_TERM_ID" ENABLE;	

--------------------------------------------------------
--  DDL for Synonymn SEARCH_TAXONOMY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_TAXONOMY" FOR "SEARCHAPP"."SEARCH_TAXONOMY";

--------------------------------------------------------
--  DDL for Synonymn SEQ_SEARCH_TAXONOMY_TERM_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SEARCH_TAXONOMY_TERM_ID" FOR "SEARCHAPP"."SEQ_SEARCH_TAXONOMY_TERM_ID";  
  
