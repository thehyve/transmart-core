--------------------------------------------------------
--  DDL for Table SEARCH_TAXONOMY_RELS
--------------------------------------------------------

  CREATE TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" 
   (	"SEARCH_TAXONOMY_RELS_ID" NUMBER(22,0) NOT NULL ENABLE, --postgres NOT NULL
"CHILD_ID" NUMBER(22,0) NOT NULL ENABLE,		--postgres NOT NULL
"PARENT_ID" NUMBER(22,0),
CONSTRAINT "SEARCH_TAXONOMY_RELS_PKEY" PRIMARY KEY ("SEARCH_TAXONOMY_RELS_ID")
   )
  TABLESPACE "BIOMART" ;
  
--------------------------------------------------------
--  DDL for Index SYS_C0056419
--------------------------------------------------------

  CREATE UNIQUE INDEX "SEARCHAPP"."SYS_C0056419" ON "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ("SEARCH_TAXONOMY_RELS_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 NOLOGGING COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
--------------------------------------------------------
--  DDL for Index U_CHILD_ID_PARENT_ID
--------------------------------------------------------

  CREATE UNIQUE INDEX "SEARCHAPP"."U_CHILD_ID_PARENT_ID" ON "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ("CHILD_ID", "PARENT_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 NOLOGGING COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
  
--------------------------------------------------------
--  Constraints for Table SEARCH_TAXONOMY_RELS
--------------------------------------------------------

--  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" MODIFY ("CHILD_ID" NOT NULL ENABLE);
 
  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ADD PRIMARY KEY ("SEARCH_TAXONOMY_RELS_ID")
  TABLESPACE "BIOMART"  ENABLE;
 
  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ADD CONSTRAINT "U_CHILD_ID_PARENT_ID" UNIQUE ("CHILD_ID", "PARENT_ID")
  TABLESPACE "BIOMART"  ENABLE;
  
--------------------------------------------------------
--  Ref Constraints for Table SEARCH_TAXONOMY_RELS
--------------------------------------------------------

  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ADD CONSTRAINT "FK_SEARCH_TAX_RELS_CHILD" FOREIGN KEY ("CHILD_ID")
	  REFERENCES "SEARCHAPP"."SEARCH_TAXONOMY" ("TERM_ID") ENABLE;
 
  ALTER TABLE "SEARCHAPP"."SEARCH_TAXONOMY_RELS" ADD CONSTRAINT "FK_SEARCH_TAX_RELS_PARENT" FOREIGN KEY ("PARENT_ID")
	  REFERENCES "SEARCHAPP"."SEARCH_TAXONOMY" ("TERM_ID") ENABLE;

--------------------------------------------------------
--  DDL for Trigger TGR_SEARCH_TAXONOMY_RELS_ID
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "SEARCHAPP"."TGR_SEARCH_TAXONOMY_RELS_ID" 
  before insert on searchapp.Search_Taxonomy_rels for each row
begin 
    If Inserting 
      Then If :New.search_taxonomy_rels_Id Is Null 
        Then Select SEQ_SEARCH_DATA_ID.nextval Into :New.search_taxonomy_rels_Id From Dual; 
      End If; 
    end if;
end;


/
ALTER TRIGGER "SEARCHAPP"."TGR_SEARCH_TAXONOMY_RELS_ID" ENABLE;

--------------------------------------------------------
--  DDL for Synonymn SEARCH_TAXONOMY_RELS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_TAXONOMY_RELS" FOR "SEARCHAPP"."SEARCH_TAXONOMY_RELS";
  
--------------------------------------------------------
--  DDL for Synonymn SEQ_SEARCH_TAXONOMY_RELS_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SEARCH_TAXONOMY_RELS_ID" FOR "SEARCHAPP"."SEQ_SEARCH_TAXONOMY_RELS_ID";
