--------------------------------------------------------
--  File created - Tuesday-May-20-2014   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Type CONCEPT_CD_TAB
--------------------------------------------------------

  CREATE OR REPLACE TYPE "BIOMART_USER"."CONCEPT_CD_TAB" IS TABLE OF VARCHAR2(50)

/
--------------------------------------------------------
--  DDL for Type PATIENTS_TAB
--------------------------------------------------------

  CREATE OR REPLACE TYPE "BIOMART_USER"."PATIENTS_TAB" IS TABLE OF NUMBER(10,0)

/
--------------------------------------------------------
--  DDL for Table BIO_MARKER_EXP_ANALYSIS_MV
--------------------------------------------------------

  CREATE TABLE "BIOMART_USER"."BIO_MARKER_EXP_ANALYSIS_MV" 
   (	"BIO_MARKER_ID" NUMBER(18,0), 
	"BIO_EXPERIMENT_ID" NUMBER(18,0), 
	"BIO_ASSAY_ANALYSIS_ID" NUMBER(18,0)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
--------------------------------------------------------
--  DDL for Table IOE_TEMP_OBJ_REG2
--------------------------------------------------------

  CREATE TABLE "BIOMART_USER"."IOE_TEMP_OBJ_REG2" 
   (	"OBJECT_SCHEMA" VARCHAR2(32 BYTE), 
	"OBJECT_NAME" VARCHAR2(32 BYTE), 
	"OBJECT_TYPE" VARCHAR2(64 BYTE), 
	"KDE_USERNAME" VARCHAR2(64 BYTE), 
	"KDE_SESSION" VARCHAR2(255 BYTE), 
	"OBJECT_CREATED_TIMESTAMP" TIMESTAMP (0)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
--------------------------------------------------------
--  DDL for Table KDE_TEMP_OBJ_REG3
--------------------------------------------------------

  CREATE TABLE "BIOMART_USER"."KDE_TEMP_OBJ_REG3" 
   (	"OBJECT_SCHEMA" VARCHAR2(32 BYTE), 
	"OBJECT_NAME" VARCHAR2(32 BYTE), 
	"OBJECT_TYPE" VARCHAR2(64 BYTE), 
	"KDE_USERNAME" VARCHAR2(64 BYTE), 
	"KDE_SESSION" VARCHAR2(255 BYTE), 
	"OBJECT_CREATED_TIMESTAMP" CHAR(15 BYTE)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
--------------------------------------------------------
--  DDL for Table PLAN_TABLE
--------------------------------------------------------

  CREATE TABLE "BIOMART_USER"."PLAN_TABLE" 
   (	"STATEMENT_ID" VARCHAR2(30 BYTE), 
	"TIMESTAMP" DATE, 
	"REMARKS" VARCHAR2(80 BYTE), 
	"OPERATION" VARCHAR2(30 BYTE), 
	"OPTIONS" VARCHAR2(255 BYTE), 
	"OBJECT_NODE" VARCHAR2(128 BYTE), 
	"OBJECT_OWNER" VARCHAR2(30 BYTE), 
	"OBJECT_NAME" VARCHAR2(30 BYTE), 
	"OBJECT_INSTANCE" NUMBER(*,0), 
	"OBJECT_TYPE" VARCHAR2(30 BYTE), 
	"OPTIMIZER" VARCHAR2(255 BYTE), 
	"SEARCH_COLUMNS" NUMBER, 
	"ID" NUMBER(*,0), 
	"PARENT_ID" NUMBER(*,0), 
	"POSITION" NUMBER(*,0), 
	"COST" NUMBER(*,0), 
	"CARDINALITY" NUMBER(*,0), 
	"BYTES" NUMBER(*,0), 
	"OTHER_TAG" VARCHAR2(255 BYTE), 
	"PARTITION_START" VARCHAR2(255 BYTE), 
	"PARTITION_STOP" VARCHAR2(255 BYTE), 
	"PARTITION_ID" NUMBER(*,0), 
	"OTHER" LONG, 
	"DISTRIBUTION" VARCHAR2(30 BYTE), 
	"CPU_COST" NUMBER(*,0), 
	"IO_COST" NUMBER(*,0), 
	"TEMP_SPACE" NUMBER(*,0)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS NOLOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" ;
--------------------------------------------------------
--  DDL for Table TOAD_PLAN_TABLE
--------------------------------------------------------

  CREATE TABLE "BIOMART_USER"."TOAD_PLAN_TABLE" 
   (	"STATEMENT_ID" VARCHAR2(30 BYTE), 
	"PLAN_ID" NUMBER, 
	"TIMESTAMP" DATE, 
	"REMARKS" VARCHAR2(4000 BYTE), 
	"OPERATION" VARCHAR2(30 BYTE), 
	"OPTIONS" VARCHAR2(255 BYTE), 
	"OBJECT_NODE" VARCHAR2(128 BYTE), 
	"OBJECT_OWNER" VARCHAR2(30 BYTE), 
	"OBJECT_NAME" VARCHAR2(30 BYTE), 
	"OBJECT_ALIAS" VARCHAR2(65 BYTE), 
	"OBJECT_INSTANCE" NUMBER(*,0), 
	"OBJECT_TYPE" VARCHAR2(30 BYTE), 
	"OPTIMIZER" VARCHAR2(255 BYTE), 
	"SEARCH_COLUMNS" NUMBER, 
	"ID" NUMBER(*,0), 
	"PARENT_ID" NUMBER(*,0), 
	"DEPTH" NUMBER(*,0), 
	"POSITION" NUMBER(*,0), 
	"COST" NUMBER(*,0), 
	"CARDINALITY" NUMBER(*,0), 
	"BYTES" NUMBER(*,0), 
	"OTHER_TAG" VARCHAR2(255 BYTE), 
	"PARTITION_START" VARCHAR2(255 BYTE), 
	"PARTITION_STOP" VARCHAR2(255 BYTE), 
	"PARTITION_ID" NUMBER(*,0), 
	"OTHER" LONG, 
	"DISTRIBUTION" VARCHAR2(30 BYTE), 
	"CPU_COST" NUMBER(*,0), 
	"IO_COST" NUMBER(*,0), 
	"TEMP_SPACE" NUMBER(*,0), 
	"ACCESS_PREDICATES" VARCHAR2(4000 BYTE), 
	"FILTER_PREDICATES" VARCHAR2(4000 BYTE), 
	"PROJECTION" VARCHAR2(4000 BYTE), 
	"TIME" NUMBER(*,0), 
	"QBLOCK_NAME" VARCHAR2(30 BYTE), 
	"OTHER_XML" CLOB
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS NOLOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" 
 LOB ("OTHER_XML") STORE AS BASICFILE (
  TABLESPACE "BIOMART" ENABLE STORAGE IN ROW CHUNK 8192 RETENTION 
  NOCACHE NOLOGGING 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)) ;
--------------------------------------------------------
--  DDL for Index IDX_BIO_MARKER_EXP_ANALYSIS_MV
--------------------------------------------------------

  CREATE INDEX "BIOMART_USER"."IDX_BIO_MARKER_EXP_ANALYSIS_MV" ON "BIOMART_USER"."BIO_MARKER_EXP_ANALYSIS_MV" ("BIO_MARKER_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 NOLOGGING COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "BIOMART" 
  PARALLEL ;
--------------------------------------------------------
--  Constraints for Table BIO_MARKER_EXP_ANALYSIS_MV
--------------------------------------------------------

  ALTER TABLE "BIOMART_USER"."BIO_MARKER_EXP_ANALYSIS_MV" MODIFY ("BIO_MARKER_ID" NOT NULL ENABLE);
 
  ALTER TABLE "BIOMART_USER"."BIO_MARKER_EXP_ANALYSIS_MV" MODIFY ("BIO_ASSAY_ANALYSIS_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  DDL for Function BIO_CONCEPT_CODE_UID
--------------------------------------------------------

  CREATE OR REPLACE FUNCTION "BIOMART_USER"."BIO_CONCEPT_CODE_UID" (
  CODE_TYPE_NAME VARCHAR2,
  BIO_CONCEPT_CODE VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_concept_code.

  RETURN nvl(CODE_TYPE_NAME,'ERROR') || ':' || nvl(BIO_CONCEPT_CODE, 'ERROR');
END bio_concept_code_uid;

/
--------------------------------------------------------
--  DDL for Procedure CREATE_FILE_FROM_QUERY
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."CREATE_FILE_FROM_QUERY" ( p_query in varchar2,
                                  p_dir   in varchar2,
                                  p_filename in varchar2 )
IS
     l_output        utl_file.file_type;
     l_theCursor     INTEGER DEFAULT dbms_sql.open_cursor;
     l_columnValue   VARCHAR2(4000);
     l_status        INTEGER;
     l_query         VARCHAR2(1000);
     l_colCnt        NUMBER := 0;
     l_separator     VARCHAR2(1);
     l_descTbl       dbms_sql.desc_tab2;
BEGIN
     l_output := utl_file.fopen( p_dir, p_filename, 'w', 32767);
     EXECUTE IMMEDIATE 'alter session set nls_date_format=''dd-mon-yyyy hh24:mi:ss'' ';
     
     dbms_sql.parse(  l_theCursor,  p_query, dbms_sql.NATIVE );
     dbms_sql.describe_columns2( l_theCursor, l_colCnt, l_descTbl );
     for i in 1 .. l_colCnt loop
         utl_file.put( l_output, l_separator || '"' || l_descTbl(i).col_name|| '"' );
         dbms_output.put_line('Column Type :: ' || l_descTbl(i).col_type);
         --col_type = 112 : 112 is the # for CLOB data-type
         IF (l_desctbl(i).col_type = 112) THEN
            dbms_sql.define_column( l_theCursor, i, l_columnValue, 4000000000);
         else
            dbms_sql.define_column( l_theCursor, i, l_columnValue, 4000 );
         END IF;
         l_separator := ',';
     end loop;
     utl_file.new_line( l_output );
     l_status := dbms_sql.execute(l_theCursor);
     while ( dbms_sql.fetch_rows(l_theCursor) > 0 ) loop
         l_separator := '';
         FOR i IN 1 .. l_colCnt loop
             dbms_sql.column_value( l_theCursor, i, l_columnValue );
             IF (l_desctbl(i).col_type = 112) THEN
                l_columnValue := rtrim(rtrim(dbms_lob.substr(replace(l_columnValue,'"','""'))));
             END IF;
             utl_file.put( l_output, l_separator || l_columnValue );
             l_separator := ',';
         end loop;
         utl_file.new_line( l_output );
     end loop;
     dbms_sql.close_cursor(l_theCursor);
     utl_file.fclose( l_output );
     execute immediate 'alter session set nls_date_format=''dd-MON-yy'' ';
exception
    when utl_file.invalid_path then
       raise_application_error(-20100,'Invalid Path');
    when utl_file.invalid_mode then
       raise_application_error(-20101,'Invalid Mode');
    when utl_file.invalid_operation then
       raise_application_error(-20102,'Invalid Operation');
    when utl_file.invalid_filehandle then
       raise_application_error(-20103,'Invalid FileHandle');
    when utl_file.write_error then
       raise_application_error(-20104,'Write Error');
    when utl_file.read_error then
       raise_application_error(-20105,'Read Error');
    when utl_file.internal_error then
       raise_application_error(-20106,'Internal Error');
    when others then
         utl_file.fclose( l_output );
         execute immediate 'alter session set nls_date_format=''dd-MON-yy'' ';
         raise;
end;
 
 
 

/
--------------------------------------------------------
--  DDL for Procedure CZ_WRITE_AUDIT
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."CZ_WRITE_AUDIT" 
(
	jobId IN NUMBER,
	databaseName IN VARCHAR2 , 
	procedureName IN VARCHAR2 , 
	stepDesc IN VARCHAR2 , 
	recordsManipulated IN NUMBER,
	stepNumber IN NUMBER,
	stepStatus IN VARCHAR2
)
AS
  lastTime timestamp;
BEGIN
  select max(job_date)
    into lastTime
    from cz_job_audit
    where job_id = jobID;

	insert 	into cz_job_audit(
		job_id, 
		database_name,
 		procedure_name, 
 		step_desc, 
		records_manipulated,
		step_number,
		step_status,
    job_date,
    time_elapsed_secs
	)
	select
 		jobId,
		databaseName,
		procedureName,
		stepDesc,
		recordsManipulated,
		stepNumber,
		stepStatus,
    SYSTIMESTAMP,
      COALESCE(
      EXTRACT (DAY    FROM (SYSTIMESTAMP - lastTime))*24*60*60 + 
      EXTRACT (HOUR   FROM (SYSTIMESTAMP - lastTime))*60*60 + 
      EXTRACT (MINUTE FROM (SYSTIMESTAMP - lastTime))*60 + 
      EXTRACT (SECOND FROM (SYSTIMESTAMP - lastTime))
      ,0)
  from dual;
  
  COMMIT;

END;

/
--------------------------------------------------------
--  DDL for Procedure I2B2_LOAD_EQTL_TOP50
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."I2B2_LOAD_EQTL_TOP50" 
(i_bio_assay_analysis_id number
,i_job_id        number := null
)
 AS 
        --Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);
        
        v_sqlText                varchar2(2000);
        
BEGIN
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := i_job_id;

    SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
    procedureName := $$PLSQL_UNIT;

    --Audit JOB Initialization
    --If Job ID does not exist, then this is a single procedure run and we need to create it
    IF(jobID IS NULL or jobID < 1)
    THEN
        newJobFlag := 1; -- True
        cz_start_audit (procedureName, databaseName, jobID);
    END IF;
        
    stepCt := 1;    
    cz_write_audit(jobID,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done');
    
        --        delete existing data from bio_asy_analysis_eqtl_top50
        
        delete from biomart.bio_asy_analysis_eqtl_top50
        where bio_assay_analysis_id = i_bio_assay_analysis_id;
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Delete data for analysis from biomart.bio_asy_analysis_eqtls_top50',SQL%ROWCOUNT,stepCt,'Done');
        commit; 
        
/*
        --        disable indexes 

        for eqtl_idx in (select index_name
                                                        ,table_name
                                         from all_indexes 
                                         where owner = 'BIOMART' 
                                           and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50')
        loop
                v_sqlText := 'alter index ' || eqtl_idx.index_name || ' unusable';
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,0,stepCt,'Done');
                execute immediate(v_sqlText);
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling complete',0,stepCt,'Done');       
        end loop;
*/
        
        --        insert analysis into bio_asy_analysis_eqtl_top50
        
        insert into biomart.bio_asy_analysis_eqtl_top50
        (bio_assay_analysis_id
        ,analysis
        ,chrom
        ,pos
        ,rsgene
        ,rsid
        ,pvalue
        ,logpvalue
        ,extdata
        ,rnum
        ,intronexon
        ,recombinationrate
        ,regulome
        )
        select a.bio_assay_analysis_id
                  ,a.analysis
                  ,info.chrom
                  ,info.pos
                  ,a.gene
                  ,a.rsid
                  ,a.pvalue
                  ,a.logpvalue
                  ,a.extdata
                  ,a.rnum
                  ,info.exon_intron as intronexon
                  ,info.recombination_rate as recombinationrate
                  ,info.regulome_score as regulome
        from (select b.bio_assay_analysis_id
                                ,b.analysis 
                                ,b.rsid
                                ,b.pvalue
                                ,b.logpvalue
                                ,b.extdata
                                ,b.gene
                                ,b.cis_trans
                                ,b.distance_from_gene
                                ,b.rnum
                 from (select eqtl.bio_assay_analysis_id
                                         ,baa.analysis_name as analysis
                                         ,eqtl.rs_id as rsid
                                         ,eqtl.p_value as pvalue
                                         ,eqtl.log_p_value as logpvalue
                                         ,eqtl.ext_data as extdata
                                         ,eqtl.gene
                                         ,eqtl.cis_trans
                                         ,eqtl.distance_from_gene
                                         ,row_number () over (order by eqtl.p_value asc, eqtl.rs_id asc) as rnum
                          from biomart.bio_assay_analysis_eqtl eqtl 
                          inner join biomart.bio_assay_analysis baa 
                                        on  baa.bio_assay_analysis_id = eqtl.bio_assay_analysis_id
                          where eqtl.bio_assay_analysis_id = i_bio_assay_analysis_id) b
                 where b.rnum < 500) a          
        inner join deapp.de_rc_snp_info info 
                  on  a.rsid = info.rs_id 
                  and hg_version='19';
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Insert data for analysis from biomart.bio_asy_analysis_eaqtl_top50',SQL%ROWCOUNT,stepCt,'Done');
        commit; 

/*
        --        rebuild indexes
        
        for eqtl_idx in (select index_name 
                                                           ,table_name
                                                 from all_indexes 
                                                 where owner = 'BIOMART' 
                                                   and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50')
                loop
                        v_sqlText := 'alter index ' || eqtl_idx.index_name || ' rebuild';
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,0,stepCt,'Done');
                        execute immediate(v_sqlText);
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding complete',SQL%ROWCOUNT,stepCt,'Done');       
                end loop;
*/

        cz_write_audit(jobID,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done');
    stepCt := stepCt + 1;
    
    cz_end_audit(jobID, 'Success');
    
    exception
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');
                
END I2B2_LOAD_eqtl_TOP50;        

/*
execute immediate ('drop table biomart.tmp_analysis_count_eqtl');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.tmp_analysis_count_eqtl as
select count(*) as total, bio_assay_analysis_id
from biomart.bio_assay_analysis_eqtl
group by bio_assay_analysis_id');


execute immediate ('update biomart.bio_assay_analysis b
set b.data_count = (select a.total from biomart.tmp_analysis_count_eqtl  a where a.bio_assay_analysis_id =  b.bio_assay_analysis_id)
where exists(
select 1 from biomart.tmp_analysis_count_eqtl  a where a.bio_assay_analysis_id =  b.bio_assay_analysis_id
)');

--select * from bio_assay_analysis_eqtl 
--where bio_assay_analysis_id = 419842521
--order by p_value asc, rs_id asc;

--select * from tmp_analysis_eqtl_top500
--where bio_assay_analysis_id = 419842521
--order by p_value asc;

BEGIN
execute immediate ('drop table biomart.tmp_analysis_eqtl_top500');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.tmp_analysis_eqtl_top500 
as
select a.* 
from (
select 
bio_asy_analysis_eqtl_id,
bio_assay_analysis_id,
rs_id,
p_value,
log_p_value,
etl_id,
ext_data,
p_value_char,
gene,
cis_trans,
distance_from_gene,
row_number () over (partition by bio_assay_analysis_id order by p_value asc, rs_id asc) as rnum
from biomart.bio_assay_analysis_eqtl
) a
where 
a.rnum <=500');

execute immediate ('create index BIOMART.t_a_ge_t500_idx on BIOMART.TMP_ANALYSIS_eqtl_TOP500(RS_ID) tablespace "INDX"');
execute immediate ('create index BIOMART.t_a_gae_t500_idx on BIOMART.TMP_ANALYSIS_eqtl_TOP500(bio_assay_analysis_id) tablespace "INDX"');

BEGIN
execute immediate ('drop table biomart.bio_asy_analysis_eqtl_top50 cascade constraints');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.BIO_ASY_ANALYSIS_eqtl_TOP50
as 
SELECT baa.bio_assay_analysis_id,
baa.analysis_name AS analysis, info.chrom AS chrom, info.pos AS pos,
gmap.gene_name AS rsgene, DATA.rs_id AS rsid,
DATA.p_value AS pvalue, DATA.log_p_value AS logpvalue, data.gene as gene,
DATA.ext_data AS extdata , DATA.rnum,
info.exon_intron as intronexon, info.recombination_rate as recombinationrate, info.regulome_score as regulome
FROM biomart.tmp_analysis_eqtl_top500 DATA 
JOIN biomart.bio_assay_analysis baa 
ON baa.bio_assay_analysis_id = DATA.bio_assay_analysis_id
JOIN deapp.de_rc_snp_info info ON DATA.rs_id = info.rs_id and (hg_version='''||19||''')
LEFT JOIN deapp.de_snp_gene_map gmap ON  gmap.snp_name =info.rs_id');

--execute immediate ('select count(*) from BIO_ASY_ANALYSIS_eqtl_TOP50');

execute immediate ('create index BIOMART.B_ASY_eqtl_T50_IDX1 on BIOMART.BIO_ASY_ANALYSIS_eqtl_TOP50(bio_assay_analysis_id) parallel tablespace "INDX"');

execute immediate ('create index BIOMART.B_ASY_eqtl_T50_IDX2 on BIOMART.BIO_ASY_ANALYSIS_eqtl_TOP50(ANALYSIS) parallel tablespace "INDX"');

END I2B2_LOAD_EQTL_TOP50;

*/

/
--------------------------------------------------------
--  DDL for Procedure I2B2_LOAD_GWAS_TOP50
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."I2B2_LOAD_GWAS_TOP50" 
(i_bio_assay_analysis_id number
,i_job_id        number := null
)
 AS 
        --Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);
        
        v_sqlText                varchar2(2000);
        
BEGIN
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := i_job_id;

    SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
    procedureName := $$PLSQL_UNIT;

    --Audit JOB Initialization
    --If Job ID does not exist, then this is a single procedure run and we need to create it
    IF(jobID IS NULL or jobID < 1)
    THEN
        newJobFlag := 1; -- True
        cz_start_audit (procedureName, databaseName, jobID);
    END IF;
        
    stepCt := 1;    
    cz_write_audit(jobID,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done');
    
        --        delete existing data from bio_asy_analysis_gwas_top50
        
        delete from biomart.bio_asy_analysis_gwas_top50
        where bio_assay_analysis_id = i_bio_assay_analysis_id;
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Delete data for analysis from biomart.bio_asy_analysis_gwas_top50',SQL%ROWCOUNT,stepCt,'Done');
        commit; 
        
/*
        --        disable indexes 

        for gwas_idx in (select index_name
                                                        ,table_name
                                         from all_indexes 
                                         where owner = 'BIOMART' 
                                           and table_name = 'BIO_ASY_ANALYSIS_gwas_TOP50')
        loop
                v_sqlText := 'alter index ' || gwas_idx.index_name || ' unusable';
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling index ' || gwas_idx.index_name || ' on ' || gwas_idx.table_name,0,stepCt,'Done');
                execute immediate(v_sqlText);
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling complete',0,stepCt,'Done');       
        end loop;
*/
        
        --        insert analysis into bio_asy_analysis_gwas_top50
        
        insert into biomart.bio_asy_analysis_gwas_top50
        (bio_assay_analysis_id
        ,analysis
        ,chrom
        ,pos
        ,rsgene
        ,rsid
        ,pvalue
        ,logpvalue
        ,extdata
        ,rnum
        ,intronexon
        ,recombinationrate
        ,regulome
        )
        select a.bio_assay_analysis_id
                  ,a.analysis
                  ,info.chrom
                  ,info.pos
                  ,info.gene_name AS rsgene
                  ,a.rsid
                  ,a.pvalue
                  ,a.logpvalue
                  ,a.extdata
                  ,a.rnum
                  ,info.exon_intron as intronexon
                  ,info.recombination_rate as recombinationrate
                  ,info.regulome_score as regulome
        from (select b.bio_assay_analysis_id
                                ,b.analysis 
                                ,b.rsid
                                ,b.pvalue
                                ,b.logpvalue
                                ,b.extdata
                                ,b.rnum
                 from (select gwas.bio_assay_analysis_id
                                         ,baa.analysis_name as analysis
                                         ,gwas.rs_id as rsid
                                         ,gwas.p_value as pvalue
                                         ,gwas.log_p_value as logpvalue
                                         ,gwas.ext_data as extdata
                                         ,row_number () over (order by gwas.p_value asc, gwas.rs_id asc) as rnum
                          from biomart.bio_assay_analysis_gwas gwas 
                          inner join biomart.bio_assay_analysis baa 
                                        on  baa.bio_assay_analysis_id = gwas.bio_assay_analysis_id
                          where gwas.bio_assay_analysis_id = i_bio_assay_analysis_id) b
                 where b.rnum < 500) a          
        inner join deapp.de_rc_snp_info info 
                  on  a.rsid = info.rs_id 
                  and hg_version='19';
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Insert data for analysis from biomart.bio_asy_analysis_gwas_top50',0,stepCt,'Done');
        commit; 
        
     /*   for gwas_idx in (select index_name 
                                                           ,table_name
                                                 from all_indexes 
                                                 where owner = 'BIOMART' 
                                                   and table_name = 'BIO_ASY_ANALYSIS_GWAS_TOP50')
                loop
                        v_sqlText := 'alter index ' || gwas_idx.index_name || ' rebuild';
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding index ' || gwas_idx.index_name || ' on ' || gwas_idx.table_name,0,stepCt,'Done');
                        execute immediate(v_sqlText);
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding complete',SQL%ROWCOUNT,stepCt,'Done');       
                end loop;
        */
        cz_write_audit(jobID,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done');
    stepCt := stepCt + 1;
    
    cz_end_audit(jobID, 'Success');
    
    exception
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');

END I2B2_LOAD_GWAS_TOP50;

/*
--select * from bio_assay_analysis_gwas 
--where bio_assay_analysis_id = 419842521
--order by p_value asc, rs_id asc;

--select * from tmp_analysis_gwas_top500
--where bio_assay_analysis_id = 419842521
-- order by p_value asc;

BEGIN
execute immediate('drop table BIOMART.tmp_analysis_gwas_top500');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate('create table BIOMART.tmp_analysis_gwas_top500 
as
select a.* 
from (
select 
bio_asy_analysis_gwas_id,
bio_assay_analysis_id,
rs_id,
p_value,
log_p_value,
etl_id,
ext_data,
p_value_char,
row_number () over (partition by bio_assay_analysis_id order by p_value asc, rs_id asc) as rnum
from BIOMART.bio_assay_analysis_gwas
) a
where 
a.rnum <=500');

execute immediate('create index t_a_g_t500_idx on BIOMART.TMP_ANALYSIS_GWAS_TOP500(RS_ID) tablespace "INDX"');
execute immediate('create index t_a_ga_t500_idx on BIOMART.TMP_ANALYSIS_GWAS_TOP500(bio_assay_analysis_id) tablespace "INDX"');

BEGIN
execute immediate('drop table BIOMART.bio_asy_analysis_gwas_top50');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate('create table BIOMART.BIO_ASY_ANALYSIS_GWAS_TOP50
as 
SELECT baa.bio_assay_analysis_id,
baa.analysis_name AS analysis, info.chrom AS chrom, info.pos AS pos,
gmap.gene_name AS rsgene, DATA.rs_id AS rsid,
DATA.p_value AS pvalue, DATA.log_p_value AS logpvalue,
DATA.ext_data AS extdata , DATA.rnum,
info.exon_intron as intronexon, info.recombination_rate as recombinationrate, info.regulome_score as regulome
FROM biomart.tmp_analysis_gwas_top500 DATA 
JOIN biomart.bio_assay_analysis baa 
ON baa.bio_assay_analysis_id = DATA.bio_assay_analysis_id
JOIN deapp.de_rc_snp_info info ON DATA.rs_id = info.rs_id and (hg_version='''||19||''')
LEFT JOIN deapp.de_snp_gene_map gmap ON  gmap.snp_name =info.rs_id') ;

--select count(*) from BIO_ASY_ANALYSIS_GWAS_TOP50;

execute immediate('create index BIOMART.B_ASY_GWAS_T50_IDX1 on BIOMART.BIO_ASY_ANALYSIS_GWAS_TOP50(bio_assay_analysis_id) parallel tablespace "INDX"');

execute immediate('create index BIOMART.B_ASY_GWAS_T50_IDX2 on BIOMART.BIO_ASY_ANALYSIS_GWAS_TOP50(ANALYSIS) parallel tablespace "INDX"');
*/

/
--------------------------------------------------------
--  DDL for Procedure I2B2_MOVE_ANALYSIS_TO_PROD_NEW
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."I2B2_MOVE_ANALYSIS_TO_PROD_NEW" 
(i_etl_id        number    := -1
,i_job_id        number    := null
)
AS
  
    --Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);
    
    v_etl_id                    number(18,0);
    v_bio_assay_analysis_id        number(18,0);
    v_data_type                    varchar2(50);
    v_sqlText                    varchar2(2000);
    v_exists                    int;
    v_GWAS_staged                int;
    v_EQTL_staged                int;
    
    type stage_rec  is record
    (bio_assay_analysis_id        number(18,0)
    ,etl_id                        number(18,0)
    ,study_id                    varchar2(500)
    ,data_type                    varchar2(50)
    ,orig_data_type                varchar2(50)
    ,analysis_name                varchar2(1000)
    );

    type stage_table is table of stage_rec; 
    stage_array stage_table;
    
    type stage_table_names_rec is record
    (table_name                    varchar2(500)
    );
    
    type stage_table_names is table of stage_table_names_rec;
    stage_table_array stage_table_names;
    
    no_staged_data    exception;
    
    BEGIN    
    
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := -1;

    SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
    procedureName := $$PLSQL_UNIT;

    --Audit JOB Initialization
    --If Job ID does not exist, then this is a single procedure run and we need to create it
    IF(jobID IS NULL or jobID < 1)
    THEN
        newJobFlag := 1; -- True
        cz_start_audit (procedureName, databaseName, jobID);
    END IF;
        
    stepCt := 1;    
    cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_move_analysis_to_prod',0,stepCt,'Done');
    
    --    load staged analysis to array
    
    select baa.bio_assay_analysis_id
          ,lz.etl_id
          ,lz.study_id
          ,case when lz.data_type = 'Metabolic GWAS' then 'GWAS' else lz.data_type end as data_type
          ,lz.data_type as orig_data_type
          ,lz.analysis_name
    bulk collect into stage_array
    from tm_lz.lz_src_analysis_metadata lz
        ,biomart.bio_assay_analysis baa
    where lz.status = 'STAGED'
      and lz.study_id = baa.etl_id
      and lz.etl_id = baa.etl_id_source
      and case when i_etl_id = -1 then 1
               when lz.etl_id = i_etl_id then 1
               else 0 end = 1;
               
    v_exists := SQL%ROWCOUNT;
    
    if v_exists = 0 then
        raise no_staged_data;
    end if;

    --    set variables if staged data contains GWAS and/or EQTL data
    
    v_GWAS_staged := 0;
    v_EQTL_staged := 0;
    
    for i in stage_array.first .. stage_array.last
    loop    
        if stage_array(i).data_type = 'GWAS' then
            v_GWAS_staged := 1;
        end if;
        
        if stage_array(i).data_type = 'EQTL' then
            v_EQTL_staged := 1;
        end if;    
        
    end loop;
    
    --    drop indexes if loading GWAS data
    
    if v_GWAS_staged = 1 then
        select count(*) into v_exists
        from all_indexes
        where owner = 'BIOMART'
          and table_name = 'BIO_ASSAY_ANALYSIS_GWAS'
          and index_name = 'BIO_ASSAY_ANALYSIS_GWAS_PK';
          
        if v_exists > 0 then
            execute immediate('drop index biomart.bio_assay_analysis_gwas_pk');
        end if;

        select count(*) into v_exists
        from all_indexes
        where owner = 'BIOMART'
          and table_name = 'BIO_ASSAY_ANALYSIS_GWAS'
          and index_name = 'BIO_ASSAY_ANALYSIS_GWAS_IDX2';
          
        if v_exists > 0 then
            execute immediate('drop index biomart.bio_assay_analysis_gwas_idx2');
        end if;        
    end if;

    --    delete any existing data in bio_assay_analysis_gwas and bio_assay_analysis_eqtl
               
    if v_GWAS_staged = 1 then
        delete from biomart.bio_assay_analysis_gwas g
        where g.bio_assay_analysis_id in
             (select x.bio_assay_analysis_id
              from tm_lz.lz_src_analysis_metadata t
                  ,biomart.bio_assay_analysis x
              where t.status = 'STAGED'
                and t.data_type in ('GWAS','Metabolic GWAS')
                and t.study_id = x.etl_id
                and t.etl_id = x.etl_id_source
                and case when i_etl_id = -1 then 1
                         when t.etl_id = i_etl_id then 1
                         else 0 end = 1);
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Delete exising data for staged analyses from BIOMART.BIO_ASSAY_ANALYSIS_GWAS',SQL%ROWCOUNT,stepCt,'Done');
        commit;    
    end if;

    if v_EQTL_staged = 1 then
        delete from biomart.bio_assay_analysis_eqtl g
        where g.bio_assay_analysis_id in
             (select x.bio_assay_analysis_id
              from tm_lz.lz_src_analysis_metadata t
                  ,biomart.bio_assay_analysis x
              where t.status = 'STAGED'
                and t.data_type = 'EQTL'
                and t.study_id = x.etl_id
                and t.etl_id = x.etl_id_source
                and case when i_etl_id = -1 then 1
                         when t.etl_id = i_etl_id then 1
                         else 0 end = 1);
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Delete exising data for staged analyses from BIOMART.BIO_ASSAY_ANALYSIS_EQTL',SQL%ROWCOUNT,stepCt,'Done');
        commit;    
    end if;
    
    if v_GWAS_staged = 1 then
        select count(*) into v_exists
        from all_indexes
        where owner = 'BIOMART'
          and table_name = 'BIO_ASSAY_ANALYSIS_GWAS'
          and index_name = 'BIO_ASSAY_ANALYSIS_GWAS_IDX1';
          
        if v_exists > 0 then
            execute immediate('drop index biomart.BIO_ASSAY_ANALYSIS_GWAS_IDX1');
        end if;    
    end if;
    
    for i in stage_array.first .. stage_array.last
    loop
        
        cz_write_audit(jobId,databaseName,procedureName,'Loading ' || stage_array(i).study_id || ' ' || stage_array(i).orig_data_type || ' ' ||
                       stage_array(i).analysis_name,0,stepCt,'Done');
                       
        v_etl_id := stage_array(i).etl_id;
        v_bio_assay_analysis_id := stage_array(i).bio_assay_analysis_id;
        v_data_type := stage_array(i).data_type;
        
        if v_data_type = 'EQTL' then
            insert into biomart.bio_assay_analysis_eqtl
            (bio_asy_analysis_eqtl_id
            ,bio_assay_analysis_id
            ,rs_id
            ,gene
            ,p_value
            ,p_value_char
            ,cis_trans
            ,distance_from_gene
            ,etl_id
            ,ext_data
            ,log_p_value)
            select bio_asy_analysis_eqtl_id
                  ,bio_assay_analysis_id
                  ,rs_id
                  ,gene
                  ,to_binary_double(p_value_char)
                  ,p_value_char
                  ,cis_trans
                  ,distance_from_gene
                  ,etl_id
                  ,ext_data
                  ,log(10,to_binary_double(p_value_char))*-1
            from biomart_stage.bio_assay_analysis_eqtl
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,SQL%ROWCOUNT,stepCt,'Done');
         
            commit;        
        else
            insert into biomart.bio_assay_analysis_gwas
            (bio_asy_analysis_gwas_id
            ,bio_assay_analysis_id
            ,rs_id
            ,p_value
            ,p_value_char
            ,etl_id
            ,ext_data
            ,log_p_value)
            select bio_asy_analysis_gwas_id
                  ,bio_assay_analysis_id
                  ,rs_id
                  ,to_binary_double(p_value_char)
                  ,p_value_char
                  ,etl_id
                  ,ext_data
                  ,log(10,to_binary_double(p_value_char))*-1
            from biomart_stage.bio_assay_analysis_gwas
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,SQL%ROWCOUNT,stepCt,'Done');
            commit;    
        end if;

        if i_etl_id > -1 then

            v_sqlText := 'delete from biomart_stage.bio_assay_analysis_' || v_data_type || 
                         ' where bio_assay_analysis_id = ' || to_char(v_bio_assay_analysis_id);
            --dbms_output.put_line(v_sqlText);
            execute immediate(v_sqlText);
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,SQL%ROWCOUNT,stepCt,'Done');
            commit;    
        end if;    
        
        update tm_lz.lz_src_analysis_metadata
        set status='PRODUCTION'
        where etl_id = v_etl_id;
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Set status to PRODUCTION in tm_lz.lz_src_analysis_metadata',SQL%ROWCOUNT,stepCt,'Done');
        commit;                
            
    end loop;
    
    if i_etl_id = -1 then
    
        select table_name
        bulk collect into stage_table_array
        from all_tables
        where owner = 'BIOMART_STAGE'
          and table_name like 'BIO_ASSAY_ANALYSIS%';
          
        for i in stage_table_array.first .. stage_table_array.last
        loop
            v_sqlText := 'truncate table biomart_stage.' || stage_table_array(i).table_name;
            --dbms_output.put_line(v_sqlText);
            execute immediate(v_sqlText);
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Truncated biomart_stage.' || stage_table_array(i).table_name,0,stepCt,'Done');
        end loop;
    end if;
    
    --    recreate GWAS indexes if needed
    
    if v_GWAS_staged = 1 then
        execute immediate('create index biomart.bio_assay_analysis_gwas_idx1 on biomart.bio_assay_analysis_gwas (bio_assay_analysis_id) tablespace "INDX" parallel 8');
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Created index bio_assay_analysis_gwas_idx1',0,stepCt,'Done');
        execute immediate('create index biomart.bio_assay_analysis_gwas_idx2 on biomart.bio_assay_analysis_gwas (rs_id) tablespace "INDX" parallel 8');
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Created index bio_assay_analysis_gwas_idx2',0,stepCt,'Done');
        execute immediate('create unique index biomart.bio_assay_analysis_gwas_pk on biomart.bio_assay_analysis_gwas (bio_asy_analysis_gwas_id) tablespace "INDX" parallel 8 ');
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Created index bio_assay_analysis_gwas_pk',0,stepCt,'Done');
        
        
    I2B2_LOAD_EQTL_TOP50();
    stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Created top 50 EQTL',0,stepCt,'Done');
    I2B2_LOAD_GWAS_TOP50();
    stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Created top 50 GWAS',0,stepCt,'Done');

    end if;
    
    --Insert data_count to bio_assay_analysis table. added by Haiyan Zhang 01/22/2013
    for i in stage_array.first .. stage_array.last
    loop
        v_bio_assay_analysis_id := stage_array(i).bio_assay_analysis_id;
        v_data_type := stage_array(i).data_type;
        if v_data_type = 'EQTL' then
          
            update biomart.bio_assay_analysis set data_count=(select count(*) from biomart.bio_assay_analysis_eqtl 
            where bio_assay_analysis_eqtl.bio_assay_analysis_id=v_bio_assay_analysis_id) 
            where bio_assay_analysis.bio_assay_analysis_id=v_bio_assay_analysis_id;
            stepCt := stepCt +1;
            cz_write_audit(jobId,databaseName,procedureName,'Update data_count for analysis ' || v_data_type,SQL%ROWCOUNT,stepCt,'Done');
            commit;
        else
          
            update biomart.bio_assay_analysis set data_count=(select count(*) from biomart.bio_assay_analysis_gwas 
            where bio_assay_analysis_gwas.bio_assay_analysis_id=v_bio_assay_analysis_id) 
            where bio_assay_analysis.bio_assay_analysis_id=v_bio_assay_analysis_id;
            stepCt := stepCt +1;
            cz_write_audit(jobId,databaseName,procedureName,'Update data_count for analysis ' || v_data_type,SQL%ROWCOUNT,stepCt,'Done');
            commit;
        end if;
    end loop; 
    ---end added by Haiyan Zhang
    
    cz_write_audit(jobId,databaseName,procedureName,'End i2b2_move_analysis_to_prod',0,stepCt,'Done');
    stepCt := stepCt + 1;
    
    cz_end_audit(jobId, 'Success');
    
    exception
    when no_staged_data then
        cz_write_audit(jobId, databaseName, procedureName, 'No staged data - run terminating normally',0,stepCt,'Done');
        cz_end_audit(jobId, 'Success');
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');
    
END;

/
--------------------------------------------------------
--  DDL for Procedure PATIENT_SUBSET2
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."PATIENT_SUBSET2" (
  p_result_instance_id IN VARCHAR2,
  p_pathway IN VARCHAR2,
  p_refcur  OUT SYS_REFCURSOR) AS
  
  v_patients patients_tab;
  v_concept_cd concept_cd_tab;

BEGIN

SELECT patient_num BULK COLLECT INTO v_patients FROM (SELECT  
             DISTINCT a.patient_num
        FROM qt_patient_set_collection a, 
             qt_query_result_instance b, 
             qt_query_instance c, 
             qt_query_master d 
        WHERE a.result_instance_id = b.result_instance_id AND 
              b.query_instance_id = c.query_instance_id AND 
              c.query_master_id = d.query_master_id AND 
              b.result_instance_id = p_result_instance_id);
    
FOR record IN (SELECT SUBSTR(item_key,INSTR(item_key,'\',1,3)) AS concept_path  FROM (  
      SELECT extractValue(value(ik),'/item_key') item_key FROM (SELECT sys.xmltype.createXML(a.i2b2_request_xml) col 
        FROM qt_query_master a, 
             qt_query_instance b, 
             qt_query_result_instance c 
        WHERE a.query_master_id = b.query_master_id AND
              b.query_instance_id = c.query_instance_id AND 
              c.result_instance_id = p_result_instance_id) tab1,
              TABLE(xmlsequence(extract(col,'//ns4:request/query_definition/panel/item/item_key',                        
                                            'xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/"'))) ik))
LOOP                                            
   SELECT concept_cd BULK COLLECT INTO v_concept_cd FROM concept_dimension 
        WHERE concept_path like record.concept_path||'%';
END LOOP;
 
 
 FOR record IN (SELECT * FROM TABLE(CAST(v_concept_cd AS concept_cd_tab)))
 LOOP
 DBMS_OUTPUT.PUT_LINE(record.column_value);
 END LOOP;
 
OPEN p_refcur FOR
  SELECT DISTINCT a.probeset, a.gene_symbol, a.refseq, a.zscore, a.pvalue, b.patient_uid, a.assay_id
      FROM de_subject_assay_data a
      JOIN de_subject_sample_mapping b
        ON a.patient_id = b.patient_id
      JOIN de_pathway_gene c
        ON a.gene_symbol = c.gene_symbol
      JOIN de_pathway d
        ON d.id = c.pathway_id
      WHERE d.name = p_pathway
        AND b.patient_uid IN (SELECT * FROM TABLE(CAST(v_patients as patients_tab)))
        AND b.concept_code IN (CASE WHEN ( SELECT COUNT(*) FROM de_subject_sample_mapping 
                                       WHERE concept_code IN (SELECT * FROM TABLE(CAST(v_concept_cd AS concept_cd_tab)))) > 0 THEN
                                         (SELECT * FROM TABLE(CAST(v_concept_cd AS concept_cd_tab)))
                                       ELSE
                                         (SELECT concept_code FROM de_subject_sample_mapping)
                                      END);
                                         
END PATIENT_SUBSET2;

 
 
 
 
 
 
 

/
--------------------------------------------------------
--  DDL for Procedure TM_CZ.CZ_WRITE_AUDIT
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."TM_CZ.CZ_WRITE_AUDIT" 
(
	jobId IN NUMBER,
	databaseName IN VARCHAR2 , 
	procedureName IN VARCHAR2 , 
	stepDesc IN VARCHAR2 , 
	recordsManipulated IN NUMBER,
	stepNumber IN NUMBER,
	stepStatus IN VARCHAR2
)
AS
  lastTime timestamp;
BEGIN
  select max(job_date)
    into lastTime
    from cz_job_audit
    where job_id = jobID;

	insert 	into cz_job_audit(
		job_id, 
		database_name,
 		procedure_name, 
 		step_desc, 
		records_manipulated,
		step_number,
		step_status,
    job_date,
    time_elapsed_secs
	)
	select
 		jobId,
		databaseName,
		procedureName,
		stepDesc,
		recordsManipulated,
		stepNumber,
		stepStatus,
    SYSTIMESTAMP,
      COALESCE(
      EXTRACT (DAY    FROM (SYSTIMESTAMP - lastTime))*24*60*60 + 
      EXTRACT (HOUR   FROM (SYSTIMESTAMP - lastTime))*60*60 + 
      EXTRACT (MINUTE FROM (SYSTIMESTAMP - lastTime))*60 + 
      EXTRACT (SECOND FROM (SYSTIMESTAMP - lastTime))
      ,0)
  from dual;
  
  COMMIT;

END;

/
--------------------------------------------------------
--  DDL for Procedure UTIL_CREATE_SYNONYM_ALL
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."UTIL_CREATE_SYNONYM_ALL" 
(
	V_FROMZONE IN VARCHAR2 DEFAULT NULL ,
	V_WHATTYPE IN VARCHAR2 DEFAULT 'PROCEDURES,FUNCTIONS,TABLES,VIEWS,SEQUENCE'
)
AUTHID CURRENT_USER
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/
	--The name of the table, proc, function or view.
	V_OBJNAME VARCHAR2(50);

	--Dynamic SQL line
	V_CMDLINE VARCHAR2(200);

	--Table list
	CURSOR L_TABLE IS
		SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = UPPER(V_FROMZONE);
	--View List
	CURSOR L_VIEW IS
		SELECT VIEW_NAME FROM ALL_VIEWS WHERE OWNER = UPPER(V_FROMZONE);
	--Procedure and function list (OBJTYPE are PROCEDURE, FUNCTION, TRIGGER)
	CURSOR L_PROCEDURE (OBJTYPE VARCHAR2) IS
		SELECT DISTINCT OBJECT_NAME FROM ALL_PROCEDURES
			WHERE OWNER = UPPER(V_FROMZONE) AND OBJECT_TYPE = OBJTYPE
      AND UPPER(OBJECT_NAME) NOT LIKE 'UTIL%';
	--	Sequences
		--Procedure and function list (OBJTYPE are PROCEDURE, FUNCTION, TRIGGER)
	CURSOR L_SEQUENCE IS
		SELECT DISTINCT SEQUENCE_NAME FROM ALL_SEQUENCES
			WHERE SEQUENCE_OWNER = UPPER(V_FROMZONE);

BEGIN

	-- Create synonyms for Tables
	IF UPPER(V_WHATTYPE) LIKE '%TABLE%' THEN

		OPEN L_TABLE;
			FETCH L_TABLE INTO V_OBJNAME;
		WHILE L_TABLE%FOUND
			LOOP
			BEGIN

				V_CMDLINE := 'create or replace synonym ' || V_OBJNAME || ' for ' || UPPER(V_FROMZONE) || '.' || V_OBJNAME ;

				EXECUTE IMMEDIATE V_CMDLINE;
				--DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
	
				FETCH L_TABLE  INTO V_OBJNAME;

			EXCEPTION 
			WHEN OTHERS THEN
			BEGIN
				DBMS_OUTPUT.PUT_LINE('ERROR ' || V_CMDLINE);
				DBMS_OUTPUT.PUT_LINE(SQLERRM);
			END;
		END;
       END LOOP;
       CLOSE L_TABLE;
   end if;

	--CREATE SYNONYMS FOR VIEWS
	IF UPPER(V_WHATTYPE) LIKE '%VIEW%' THEN

		OPEN L_VIEW;
			FETCH L_VIEW INTO V_OBJNAME;
		WHILE L_VIEW%FOUND
			LOOP
			BEGIN

				V_CMDLINE := 'create or replace synonym ' || V_OBJNAME || ' for ' || UPPER(V_FROMZONE) || '.' || V_OBJNAME ;

				EXECUTE IMMEDIATE V_CMDLINE;
				--DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);

				FETCH L_VIEW INTO V_OBJNAME;

			EXCEPTION
			WHEN OTHERS THEN
			BEGIN
				DBMS_OUTPUT.PUT_LINE('ERROR ' || V_CMDLINE);
				DBMS_OUTPUT.PUT_LINE(SQLERRM);
			END;
		END;
		END LOOP;
		CLOSE L_VIEW;
   END IF;

-- CREATE SYNONYMS FOR PROCEDURES
	IF UPPER(V_WHATTYPE) LIKE '%PROCEDURE%' THEN

		OPEN L_PROCEDURE('PROCEDURE');
			FETCH L_PROCEDURE INTO V_OBJNAME;
		WHILE L_PROCEDURE%FOUND
			LOOP
			BEGIN

				V_CMDLINE := 'create or replace synonym ' || V_OBJNAME || ' for ' || UPPER(V_FROMZONE) || '.' || V_OBJNAME ;

				EXECUTE IMMEDIATE V_CMDLINE;
				--DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);

				FETCH l_procedure INTO V_OBJNAME;

			EXCEPTION
			WHEN OTHERS THEN
			BEGIN
				DBMS_OUTPUT.PUT_LINE('ERROR ' || V_CMDLINE);
				DBMS_OUTPUT.PUT_LINE(SQLERRM);
			END;
		END;
		END LOOP;
		CLOSE l_procedure;
   end if;

-- CREATE SYNONYMS FOR FUNCTIONS
	IF UPPER(V_WHATTYPE) LIKE '%FUNCTION%' THEN
		
		OPEN l_procedure('FUNCTION');
			FETCH l_procedure INTO V_OBJNAME;
		WHILE l_procedure%FOUND
			LOOP
			BEGIN

				V_CMDLINE := 'create synonym ' || V_OBJNAME || ' for ' || UPPER(V_FROMZONE) || '.' || V_OBJNAME ;

				EXECUTE IMMEDIATE V_CMDLINE;
				--DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);

				FETCH L_PROCEDURE INTO V_OBJNAME;
		
			EXCEPTION
			WHEN OTHERS THEN
			BEGIN
				DBMS_OUTPUT.PUT_LINE('ERROR ' || V_CMDLINE);
				DBMS_OUTPUT.PUT_LINE(SQLERRM);
			END;
		END;
		END LOOP;
		CLOSE L_PROCEDURE;
   END IF;
   
	-- Create synonyms for Tables
	IF UPPER(V_WHATTYPE) LIKE '%SEQUENCE%' THEN

		OPEN L_SEQUENCE;
			FETCH L_SEQUENCE INTO V_OBJNAME;
		WHILE L_SEQUENCE%FOUND
			LOOP
			BEGIN

				V_CMDLINE := 'create or replace synonym ' || V_OBJNAME || ' for ' || UPPER(V_FROMZONE) || '.' || V_OBJNAME ;

				EXECUTE IMMEDIATE V_CMDLINE;
				--DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
	
				FETCH L_TABLE  INTO V_OBJNAME;

			EXCEPTION 
			WHEN OTHERS THEN
			BEGIN
				DBMS_OUTPUT.PUT_LINE('ERROR ' || V_CMDLINE);
				DBMS_OUTPUT.PUT_LINE(SQLERRM);
			END;
		END;
       END LOOP;
       CLOSE L_SEQUENCE;
   end if;
END;

/
--------------------------------------------------------
--  DDL for Procedure UTIL_CREATE_SYNONYM_ALL_V2
--------------------------------------------------------
set define off;

  CREATE OR REPLACE PROCEDURE "BIOMART_USER"."UTIL_CREATE_SYNONYM_ALL_V2" 
(
	V_FROMZONE IN VARCHAR2 DEFAULT NULL ,
	V_WHATTYPE IN VARCHAR2 DEFAULT 'PROCEDURES,FUNCTIONS,TABLES,VIEWS,SEQUENCE'
)
AUTHID CURRENT_USER
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/
	--The name of the table, proc, function or view.
	V_OBJNAME VARCHAR2(50);

	--Dynamic SQL line
	V_CMDLINE VARCHAR2(200);

BEGIN

	-- Create synonyms for Tables
	IF UPPER(V_WHATTYPE) LIKE '%TABLE%' THEN
		for v_tab_rec in (select table_name from all_tables where owner = upper(v_fromzone) )
		loop
			V_CMDLINE := 'create or replace synonym ' || v_tab_rec.table_name || ' for ' || UPPER(V_FROMZONE) || '.' || v_tab_rec.table_name ;
      dbms_output.put_line(v_cmdline);
			EXECUTE IMMEDIATE V_CMDLINE;
			DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
		   END LOOP;
   end if;

	--CREATE SYNONYMS FOR VIEWS
	IF UPPER(V_WHATTYPE) LIKE '%VIEW%' THEN
	for v_view_rec in (select view_name from all_views where owner = upper(v_fromzone) )
	LOOP
		V_CMDLINE := 'create or replace synonym ' || v_view_rec.view_name || ' for ' || UPPER(V_FROMZONE) || '.' || v_view_rec.view_name ;
		EXECUTE IMMEDIATE V_CMDLINE;
		DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
		END LOOP;
   END IF;

-- CREATE SYNONYMS FOR PROCEDURES
	IF UPPER(V_WHATTYPE) LIKE '%PROCEDURE%' THEN
	for v_proc_rec in (SELECT DISTINCT OBJECT_NAME FROM ALL_PROCEDURES
						WHERE OWNER = UPPER(V_FROMZONE) AND OBJECT_TYPE = 'PROCEDURE'
						AND UPPER(OBJECT_NAME) NOT LIKE 'UTIL%')
	LOOP
		V_CMDLINE := 'create or replace synonym ' || v_proc_rec.object_name || ' for ' || UPPER(V_FROMZONE) || '.' || v_proc_rec.object_name ;
		EXECUTE IMMEDIATE V_CMDLINE;
		DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
		END LOOP;
   end if;

-- CREATE SYNONYMS FOR Functions
	IF UPPER(V_WHATTYPE) LIKE '%FUNCTION%' THEN
	for v_func_rec in (SELECT DISTINCT OBJECT_NAME FROM ALL_PROCEDURES
						WHERE OWNER = UPPER(V_FROMZONE) AND OBJECT_TYPE = 'FUNCTION'
						AND UPPER(OBJECT_NAME) NOT LIKE 'UTIL%')
	LOOP
		V_CMDLINE := 'create or replace synonym ' || v_func_rec.object_name || ' for ' || UPPER(V_FROMZONE) || '.' || v_func_rec.object_name ;
		EXECUTE IMMEDIATE V_CMDLINE;
		DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
		END LOOP;
   end if;
   
	-- Create synonyms for Sequence
	IF UPPER(V_WHATTYPE) LIKE '%SEQUENCE%' THEN
	for v_seq_rec in (select sequence_name from all_sequences where sequence_owner = upper(v_fromzone) )
	LOOP
		V_CMDLINE := 'create or replace synonym ' || v_seq_rec.sequence_name || ' for ' || UPPER(V_FROMZONE) || '.' || v_seq_rec.sequence_name ;
		EXECUTE IMMEDIATE V_CMDLINE;
		DBMS_OUTPUT.PUT_LINE('SUCCESS ' || V_CMDLINE);
       END LOOP;
   end if;
END;

/
--------------------------------------------------------
--  DDL for Synonymn ADD_ONTOLOGY_NODE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ADD_ONTOLOGY_NODE" FOR "I2B2METADATA"."ADD_ONTOLOGY_NODE";
--------------------------------------------------------
--  DDL for Synonymn ANNOTATION_DEAPP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ANNOTATION_DEAPP" FOR "TM_CZ"."ANNOTATION_DEAPP";
--------------------------------------------------------
--  DDL for Synonymn ANNOTATION_DEAPP_20120206
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ANNOTATION_DEAPP_20120206" FOR "TM_CZ"."ANNOTATION_DEAPP_20120206";
--------------------------------------------------------
--  DDL for Synonymn ASYNC_JOB
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ASYNC_JOB" FOR "I2B2DEMODATA"."ASYNC_JOB";
--------------------------------------------------------
--  DDL for Synonymn BIO_AD_HOC_PROPERTY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_AD_HOC_PROPERTY" FOR "BIOMART"."BIO_AD_HOC_PROPERTY";
--------------------------------------------------------
--  DDL for Synonymn BIO_ANALYSIS_ATTRIBUTE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ANALYSIS_ATTRIBUTE" FOR "BIOMART"."BIO_ANALYSIS_ATTRIBUTE";
--------------------------------------------------------
--  DDL for Synonymn BIO_ANALYSIS_ATTRIBUTE_LINEAGE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ANALYSIS_ATTRIBUTE_LINEAGE" FOR "BIOMART"."BIO_ANALYSIS_ATTRIBUTE_LINEAGE";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY" FOR "BIOMART"."BIO_ASSAY";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS" FOR "BIOMART"."BIO_ASSAY_ANALYSIS";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS_DATA" FOR "BIOMART"."BIO_ASSAY_ANALYSIS_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS_DATA_TEA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS_DATA_TEA" FOR "BIOMART"."BIO_ASSAY_ANALYSIS_DATA_TEA";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS_EQTL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS_EQTL" FOR "BIOMART"."BIO_ASSAY_ANALYSIS_EQTL";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS_EXT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS_EXT" FOR "BIOMART"."BIO_ASSAY_ANALYSIS_EXT";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_ANALYSIS_GWAS_OLD
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_ANALYSIS_GWAS_OLD" FOR "BIOMART"."BIO_ASSAY_ANALYSIS_GWAS";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_DATA" FOR "BIOMART"."BIO_ASSAY_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_DATASET
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_DATASET" FOR "BIOMART"."BIO_ASSAY_DATASET";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_DATA_ANNOTATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_DATA_ANNOTATION" FOR "BIOMART"."BIO_ASSAY_DATA_ANNOTATION";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_DATA_STATS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_DATA_STATS" FOR "BIOMART"."BIO_ASSAY_DATA_STATS";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_DATA_STATS_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_DATA_STATS_SEQ" FOR "BIOMART"."BIO_ASSAY_DATA_STATS_SEQ";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_FEATURE_GROUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_FEATURE_GROUP" FOR "BIOMART"."BIO_ASSAY_FEATURE_GROUP";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_PLATFORM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_PLATFORM" FOR "BIOMART"."BIO_ASSAY_PLATFORM";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASSAY_SAMPLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASSAY_SAMPLE" FOR "BIOMART"."BIO_ASSAY_SAMPLE";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASY_ANALYSIS_DATASET
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASY_ANALYSIS_DATASET" FOR "BIOMART"."BIO_ASY_ANALYSIS_DATASET";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASY_ANALYSIS_DATA_IDX
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASY_ANALYSIS_DATA_IDX" FOR "BIOMART"."BIO_ASY_ANALYSIS_DATA_IDX";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASY_ANALYSIS_PLTFM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASY_ANALYSIS_PLTFM" FOR "BIOMART"."BIO_ASY_ANALYSIS_PLTFM";
--------------------------------------------------------
--  DDL for Synonymn BIO_ASY_DATA_STATS_ALL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_ASY_DATA_STATS_ALL" FOR "BIOMART"."BIO_ASY_DATA_STATS_ALL";
--------------------------------------------------------
--  DDL for Synonymn BIO_CELL_LINE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CELL_LINE" FOR "BIOMART"."BIO_CELL_LINE";
--------------------------------------------------------
--  DDL for Synonymn BIO_CGDCP_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CGDCP_DATA" FOR "BIOMART"."BIO_CGDCP_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINC_TRIAL_ATTR
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINC_TRIAL_ATTR" FOR "BIOMART"."BIO_CLINC_TRIAL_ATTR";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINC_TRIAL_PT_GROUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINC_TRIAL_PT_GROUP" FOR "BIOMART"."BIO_CLINC_TRIAL_PT_GROUP";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINC_TRIAL_TIME_PT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINC_TRIAL_TIME_PT" FOR "BIOMART"."BIO_CLINC_TRIAL_TIME_PT";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINICAL_TRIAL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINICAL_TRIAL" FOR "BIOMART"."BIO_CLINICAL_TRIAL";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINICAL_TRIAL_DESIGN
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINICAL_TRIAL_DESIGN" FOR "BIOMART"."BIO_CLINICAL_TRIAL_DESIGN";
--------------------------------------------------------
--  DDL for Synonymn BIO_CLINICAL_TRIAL_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CLINICAL_TRIAL_RELEASE" FOR "TM_CZ"."BIO_CLINICAL_TRIAL_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_COMPOUND
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_COMPOUND" FOR "BIOMART"."BIO_COMPOUND";
--------------------------------------------------------
--  DDL for Synonymn BIO_CONCEPT_CODE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CONCEPT_CODE" FOR "BIOMART"."BIO_CONCEPT_CODE";
--------------------------------------------------------
--  DDL for Synonymn BIO_CONTENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CONTENT" FOR "BIOMART"."BIO_CONTENT";
--------------------------------------------------------
--  DDL for Synonymn BIO_CONTENT_REFERENCE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CONTENT_REFERENCE" FOR "BIOMART"."BIO_CONTENT_REFERENCE";
--------------------------------------------------------
--  DDL for Synonymn BIO_CONTENT_REPOSITORY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CONTENT_REPOSITORY" FOR "BIOMART"."BIO_CONTENT_REPOSITORY";
--------------------------------------------------------
--  DDL for Synonymn BIO_CURATED_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CURATED_DATA" FOR "BIOMART"."BIO_CURATED_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_CURATION_DATASET
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_CURATION_DATASET" FOR "BIOMART"."BIO_CURATION_DATASET";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_ATTRIBUTE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_ATTRIBUTE" FOR "BIOMART"."BIO_DATA_ATTRIBUTE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_COMPOUND
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_COMPOUND" FOR "BIOMART"."BIO_DATA_COMPOUND";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_COMPOUND_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_COMPOUND_RELEASE" FOR "TM_CZ"."BIO_DATA_COMPOUND_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_CORRELATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_CORRELATION" FOR "BIOMART"."BIO_DATA_CORRELATION";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_CORREL_DESCR
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_CORREL_DESCR" FOR "BIOMART"."BIO_DATA_CORREL_DESCR";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_DISEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_DISEASE" FOR "BIOMART"."BIO_DATA_DISEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_EXT_CODE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_EXT_CODE" FOR "BIOMART"."BIO_DATA_EXT_CODE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_LITERATURE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_LITERATURE" FOR "BIOMART"."BIO_DATA_LITERATURE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_OBSERVATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_OBSERVATION" FOR "BIOMART"."BIO_DATA_OBSERVATION";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_OMIC_MARKER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_OMIC_MARKER" FOR "BIOMART"."BIO_DATA_OMIC_MARKER";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_PLATFORM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_PLATFORM" FOR "BIOMART"."BIO_DATA_PLATFORM";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_TAXONOMY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_TAXONOMY" FOR "BIOMART"."BIO_DATA_TAXONOMY";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_UID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_UID" FOR "BIOMART"."BIO_DATA_UID";
--------------------------------------------------------
--  DDL for Synonymn BIO_DATA_UID_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DATA_UID_RELEASE" FOR "TM_CZ"."BIO_DATA_UID_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_DISEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_DISEASE" FOR "BIOMART"."BIO_DISEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_EXPERIMENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_EXPERIMENT" FOR "BIOMART"."BIO_EXPERIMENT";
--------------------------------------------------------
--  DDL for Synonymn BIO_EXPERIMENT_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_EXPERIMENT_RELEASE" FOR "TM_CZ"."BIO_EXPERIMENT_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_ALT_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_ALT_DATA" FOR "BIOMART"."BIO_LIT_ALT_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_AMD_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_AMD_DATA" FOR "BIOMART"."BIO_LIT_AMD_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_INH_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_INH_DATA" FOR "BIOMART"."BIO_LIT_INH_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_INT_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_INT_DATA" FOR "BIOMART"."BIO_LIT_INT_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_INT_MODEL_MV
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_INT_MODEL_MV" FOR "BIOMART"."BIO_LIT_INT_MODEL_MV";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_MODEL_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_MODEL_DATA" FOR "BIOMART"."BIO_LIT_MODEL_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_PE_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_PE_DATA" FOR "BIOMART"."BIO_LIT_PE_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_REF_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_REF_DATA" FOR "BIOMART"."BIO_LIT_REF_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_LIT_SUM_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_LIT_SUM_DATA" FOR "BIOMART"."BIO_LIT_SUM_DATA";
--------------------------------------------------------
--  DDL for Synonymn BIO_MARKER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_MARKER" FOR "BIOMART"."BIO_MARKER";
--------------------------------------------------------
--  DDL for Synonymn BIO_MARKER_CORREL_MV
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_MARKER_CORREL_MV" FOR "BIOMART"."BIO_MARKER_CORREL_MV";
--------------------------------------------------------
--  DDL for Synonymn BIO_OBSERVATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_OBSERVATION" FOR "BIOMART"."BIO_OBSERVATION";
--------------------------------------------------------
--  DDL for Synonymn BIO_PATIENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_PATIENT" FOR "BIOMART"."BIO_PATIENT";
--------------------------------------------------------
--  DDL for Synonymn BIO_PATIENT_EVENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_PATIENT_EVENT" FOR "BIOMART"."BIO_PATIENT_EVENT";
--------------------------------------------------------
--  DDL for Synonymn BIO_PATIENT_EVENT_ATTR
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_PATIENT_EVENT_ATTR" FOR "BIOMART"."BIO_PATIENT_EVENT_ATTR";
--------------------------------------------------------
--  DDL for Synonymn BIO_SAMPLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_SAMPLE" FOR "BIOMART"."BIO_SAMPLE";
--------------------------------------------------------
--  DDL for Synonymn BIO_STATS_EXP_MARKER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_STATS_EXP_MARKER" FOR "BIOMART"."BIO_STATS_EXP_MARKER";
--------------------------------------------------------
--  DDL for Synonymn BIO_SUBJECT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_SUBJECT" FOR "BIOMART"."BIO_SUBJECT";
--------------------------------------------------------
--  DDL for Synonymn BIO_TAXONOMY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."BIO_TAXONOMY" FOR "BIOMART"."BIO_TAXONOMY";
--------------------------------------------------------
--  DDL for Synonymn CATEGORY_PATH_EXCLUDED_WORDS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CATEGORY_PATH_EXCLUDED_WORDS" FOR "TM_CZ"."CATEGORY_PATH_EXCLUDED_WORDS";
--------------------------------------------------------
--  DDL for Synonymn CODE_LOOKUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CODE_LOOKUP" FOR "I2B2DEMODATA"."CODE_LOOKUP";
--------------------------------------------------------
--  DDL for Synonymn CONCEPT_COUNTS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CONCEPT_COUNTS" FOR "I2B2DEMODATA"."CONCEPT_COUNTS";
--------------------------------------------------------
--  DDL for Synonymn CONCEPT_DIMENSION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CONCEPT_DIMENSION" FOR "I2B2DEMODATA"."CONCEPT_DIMENSION";
--------------------------------------------------------
--  DDL for Synonymn CONCEPT_DIMENSION_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CONCEPT_DIMENSION_RELEASE" FOR "TM_CZ"."CONCEPT_DIMENSION_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn CONCEPT_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CONCEPT_ID" FOR "I2B2DEMODATA"."CONCEPT_ID";
--------------------------------------------------------
--  DDL for Synonymn COUNTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."COUNTER" FOR "I2B2DEMODATA"."COUNTER";
--------------------------------------------------------
--  DDL for Synonymn CRC_ANALYSIS_JOB
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CRC_ANALYSIS_JOB" FOR "I2B2HIVE"."CRC_ANALYSIS_JOB";
--------------------------------------------------------
--  DDL for Synonymn CRC_DB_LOOKUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CRC_DB_LOOKUP" FOR "I2B2HIVE"."CRC_DB_LOOKUP";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_CONCEPT_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_CONCEPT_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_CONCEPT_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_EID_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_EID_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_EID_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_PATIENT_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_PATIENT_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_PATIENT_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_PID_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_PID_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_PID_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_PROVIDER_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_PROVIDER_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_PROVIDER_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CREATE_TEMP_VISIT_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CREATE_TEMP_VISIT_TABLE" FOR "I2B2DEMODATA"."CREATE_TEMP_VISIT_TABLE";
--------------------------------------------------------
--  DDL for Synonymn CTD2_CLIN_INHIB_EFFECT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD2_CLIN_INHIB_EFFECT" FOR "BIOMART"."CTD2_CLIN_INHIB_EFFECT";
--------------------------------------------------------
--  DDL for Synonymn CTD2_DISEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD2_DISEASE" FOR "BIOMART"."CTD2_DISEASE";
--------------------------------------------------------
--  DDL for Synonymn CTD2_INHIB_DETAILS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD2_INHIB_DETAILS" FOR "BIOMART"."CTD2_INHIB_DETAILS";
--------------------------------------------------------
--  DDL for Synonymn CTD2_STUDY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD2_STUDY" FOR "BIOMART"."CTD2_STUDY";
--------------------------------------------------------
--  DDL for Synonymn CTD2_TRIAL_DETAILS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD2_TRIAL_DETAILS" FOR "BIOMART"."CTD2_TRIAL_DETAILS";
--------------------------------------------------------
--  DDL for Synonymn CTD_ALLOWED_MEDS_TREATMENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_ALLOWED_MEDS_TREATMENT" FOR "BIOMART"."CTD_ALLOWED_MEDS_TREATMENT";
--------------------------------------------------------
--  DDL for Synonymn CTD_ARM_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_ARM_VIEW" FOR "BIOMART"."CTD_ARM_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_BIOMARKER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_BIOMARKER" FOR "BIOMART"."CTD_BIOMARKER";
--------------------------------------------------------
--  DDL for Synonymn CTD_BIOMARKER_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_BIOMARKER_VIEW" FOR "BIOMART"."CTD_BIOMARKER_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_CELL_INFO_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_CELL_INFO_VIEW" FOR "BIOMART"."CTD_CELL_INFO_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_CLINICAL_CHARS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_CLINICAL_CHARS_VIEW" FOR "BIOMART"."CTD_CLINICAL_CHARS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_DISEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_DISEASE" FOR "BIOMART"."CTD_DISEASE";
--------------------------------------------------------
--  DDL for Synonymn CTD_DRUG_EFFECTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_DRUG_EFFECTS_VIEW" FOR "BIOMART"."CTD_DRUG_EFFECTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_DRUG_INHIB
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_DRUG_INHIB" FOR "BIOMART"."CTD_DRUG_INHIB";
--------------------------------------------------------
--  DDL for Synonymn CTD_DRUG_INHIBITOR_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_DRUG_INHIBITOR_VIEW" FOR "BIOMART"."CTD_DRUG_INHIBITOR_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EVENTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EVENTS_VIEW" FOR "BIOMART"."CTD_EVENTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EXPERIMENTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EXPERIMENTS_VIEW" FOR "BIOMART"."CTD_EXPERIMENTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EXPR_AFTER_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EXPR_AFTER_VIEW" FOR "BIOMART"."CTD_EXPR_AFTER_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EXPR_BASELINE_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EXPR_BASELINE_VIEW" FOR "BIOMART"."CTD_EXPR_BASELINE_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EXPR_BIO_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EXPR_BIO_VIEW" FOR "BIOMART"."CTD_EXPR_BIO_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_EXPR_SOURCE_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_EXPR_SOURCE_VIEW" FOR "BIOMART"."CTD_EXPR_SOURCE_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_FULL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_FULL" FOR "BIOMART"."CTD_FULL";
--------------------------------------------------------
--  DDL for Synonymn CTD_FULL_CLINICAL_ENDPTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_FULL_CLINICAL_ENDPTS_VIEW" FOR "BIOMART"."CTD_FULL_CLINICAL_ENDPTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_FULL_SEARCH_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_FULL_SEARCH_VIEW" FOR "BIOMART"."CTD_FULL_SEARCH_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_INCLUSION_CRITERIA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_INCLUSION_CRITERIA" FOR "BIOMART"."CTD_INCLUSION_CRITERIA";
--------------------------------------------------------
--  DDL for Synonymn CTD_PRIMARY_ENDPTS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_PRIMARY_ENDPTS" FOR "BIOMART"."CTD_PRIMARY_ENDPTS";
--------------------------------------------------------
--  DDL for Synonymn CTD_PRIMARY_ENDPTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_PRIMARY_ENDPTS_VIEW" FOR "BIOMART"."CTD_PRIMARY_ENDPTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_PRIOR_MED_USE_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_PRIOR_MED_USE_VIEW" FOR "BIOMART"."CTD_PRIOR_MED_USE_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_PULMONARY_PATH_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_PULMONARY_PATH_VIEW" FOR "BIOMART"."CTD_PULMONARY_PATH_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_QUANT_PARAMS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_QUANT_PARAMS_VIEW" FOR "BIOMART"."CTD_QUANT_PARAMS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_REFERENCE_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_REFERENCE_VIEW" FOR "BIOMART"."CTD_REFERENCE_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_RUNIN_THERAPIES_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_RUNIN_THERAPIES_VIEW" FOR "BIOMART"."CTD_RUNIN_THERAPIES_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_SECONDARY_ENDPTS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_SECONDARY_ENDPTS_VIEW" FOR "BIOMART"."CTD_SECONDARY_ENDPTS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_SEC_ENDPTS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_SEC_ENDPTS" FOR "BIOMART"."CTD_SEC_ENDPTS";
--------------------------------------------------------
--  DDL for Synonymn CTD_STATS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_STATS_VIEW" FOR "BIOMART"."CTD_STATS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_STUDY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_STUDY" FOR "BIOMART"."CTD_STUDY";
--------------------------------------------------------
--  DDL for Synonymn CTD_STUDY_DETAILS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_STUDY_DETAILS_VIEW" FOR "BIOMART"."CTD_STUDY_DETAILS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_DESIGN_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_DESIGN_VIEW" FOR "BIOMART"."CTD_TD_DESIGN_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_EXCL_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_EXCL_VIEW" FOR "BIOMART"."CTD_TD_EXCL_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_INCLUSION_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_INCLUSION_VIEW" FOR "BIOMART"."CTD_TD_INCLUSION_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_SMOKER_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_SMOKER_VIEW" FOR "BIOMART"."CTD_TD_SMOKER_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_SPONSOR_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_SPONSOR_VIEW" FOR "BIOMART"."CTD_TD_SPONSOR_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TD_STATUS_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TD_STATUS_VIEW" FOR "BIOMART"."CTD_TD_STATUS_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CTD_TREATMENT_PHASES_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CTD_TREATMENT_PHASES_VIEW" FOR "BIOMART"."CTD_TREATMENT_PHASES_VIEW";
--------------------------------------------------------
--  DDL for Synonymn CUSTOM_META
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CUSTOM_META" FOR "I2B2METADATA"."CUSTOM_META";
--------------------------------------------------------
--  DDL for Synonymn CZ_DATA_PROFILE_COLUMN_EXCLUSI
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_DATA_PROFILE_COLUMN_EXCLUSI" FOR "TM_CZ"."CZ_DATA_PROFILE_COLUMN_EXCLUSI";
--------------------------------------------------------
--  DDL for Synonymn CZ_DATA_PROFILE_COLUMN_SAMPLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_DATA_PROFILE_COLUMN_SAMPLE" FOR "TM_CZ"."CZ_DATA_PROFILE_COLUMN_SAMPLE";
--------------------------------------------------------
--  DDL for Synonymn CZ_DATA_PROFILE_STATS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_DATA_PROFILE_STATS" FOR "TM_CZ"."CZ_DATA_PROFILE_STATS";
--------------------------------------------------------
--  DDL for Synonymn CZ_FORM_LAYOUT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_FORM_LAYOUT" FOR "TM_CZ"."CZ_FORM_LAYOUT";
--------------------------------------------------------
--  DDL for Synonymn CZ_JOB_AUDIT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_JOB_AUDIT" FOR "TM_CZ"."CZ_JOB_AUDIT";
--------------------------------------------------------
--  DDL for Synonymn CZ_JOB_ERROR
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_JOB_ERROR" FOR "TM_CZ"."CZ_JOB_ERROR";
--------------------------------------------------------
--  DDL for Synonymn CZ_JOB_MASTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_JOB_MASTER" FOR "TM_CZ"."CZ_JOB_MASTER";
--------------------------------------------------------
--  DDL for Synonymn CZ_JOB_MESSAGE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_JOB_MESSAGE" FOR "TM_CZ"."CZ_JOB_MESSAGE";
--------------------------------------------------------
--  DDL for Synonymn CZ_PERSON
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_PERSON" FOR "TM_CZ"."CZ_PERSON";
--------------------------------------------------------
--  DDL for Synonymn CZ_REQUIRED_UPLOAD_FIELD
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_REQUIRED_UPLOAD_FIELD" FOR "TM_CZ"."CZ_REQUIRED_UPLOAD_FIELD";
--------------------------------------------------------
--  DDL for Synonymn CZ_TEST
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_TEST" FOR "TM_CZ"."CZ_TEST";
--------------------------------------------------------
--  DDL for Synonymn CZ_TEST_CATEGORY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_TEST_CATEGORY" FOR "TM_CZ"."CZ_TEST_CATEGORY";
--------------------------------------------------------
--  DDL for Synonymn CZ_TEST_RESULT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_TEST_RESULT" FOR "TM_CZ"."CZ_TEST_RESULT";
--------------------------------------------------------
--  DDL for Synonymn CZ_XTRIAL_CTRL_VOCAB
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_XTRIAL_CTRL_VOCAB" FOR "TM_CZ"."CZ_XTRIAL_CTRL_VOCAB";
--------------------------------------------------------
--  DDL for Synonymn CZ_XTRIAL_EXCLUSION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."CZ_XTRIAL_EXCLUSION" FOR "TM_CZ"."CZ_XTRIAL_EXCLUSION";
--------------------------------------------------------
--  DDL for Synonymn DATAMART_REPORT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DATAMART_REPORT" FOR "I2B2DEMODATA"."DATAMART_REPORT";
--------------------------------------------------------
--  DDL for Synonymn DEAPP_ANNOTATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DEAPP_ANNOTATION" FOR "DEAPP"."DEAPP_ANNOTATION";
--------------------------------------------------------
--  DDL for Synonymn DE_CONCEPT_CONTEXT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_CONCEPT_CONTEXT" FOR "DEAPP"."DE_CONCEPT_CONTEXT";
--------------------------------------------------------
--  DDL for Synonymn DE_CONTEXT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_CONTEXT" FOR "DEAPP"."DE_CONTEXT";
--------------------------------------------------------
--  DDL for Synonymn DE_CONTEXT_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_CONTEXT_SEQ" FOR "DEAPP"."DE_CONTEXT_SEQ";
--------------------------------------------------------
--  DDL for Synonymn DE_GPL_INFO
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_GPL_INFO" FOR "DEAPP"."DE_GPL_INFO";
--------------------------------------------------------
--  DDL for Synonymn DE_GPL_INFO_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_GPL_INFO_RELEASE" FOR "TM_CZ"."DE_GPL_INFO_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_MRNA_ANNOTATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_MRNA_ANNOTATION" FOR "DEAPP"."DE_MRNA_ANNOTATION";
--------------------------------------------------------
--  DDL for Synonymn DE_MRNA_ANNOTATION_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_MRNA_ANNOTATION_RELEASE" FOR "TM_CZ"."DE_MRNA_ANNOTATION_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_PARENT_CD_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_PARENT_CD_SEQ" FOR "DEAPP"."DE_PARENT_CD_SEQ";
--------------------------------------------------------
--  DDL for Synonymn DE_PATHWAY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_PATHWAY" FOR "DEAPP"."DE_PATHWAY";
--------------------------------------------------------
--  DDL for Synonymn DE_PATHWAY_GENE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_PATHWAY_GENE" FOR "DEAPP"."DE_PATHWAY_GENE";
--------------------------------------------------------
--  DDL for Synonymn DE_RC_SNP_INFO
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_RC_SNP_INFO" FOR "DEAPP"."DE_RC_SNP_INFO";
--------------------------------------------------------
--  DDL for Synonymn DE_SAVED_COMPARISON
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SAVED_COMPARISON" FOR "DEAPP"."DE_SAVED_COMPARISON";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_CALLS_BY_GSM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_CALLS_BY_GSM" FOR "DEAPP"."DE_SNP_CALLS_BY_GSM";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_COPY_NUMBER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_COPY_NUMBER" FOR "DEAPP"."DE_SNP_COPY_NUMBER";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_BY_PATIENT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_BY_PATIENT" FOR "DEAPP"."DE_SNP_DATA_BY_PATIENT";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_BY_PATIENT_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_BY_PATIENT_RELEASE" FOR "TM_CZ"."DE_SNP_DATA_BY_PATIENT_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_BY_PROBE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_BY_PROBE" FOR "DEAPP"."DE_SNP_DATA_BY_PROBE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_BY_PROBE_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_BY_PROBE_RELEASE" FOR "TM_CZ"."DE_SNP_DATA_BY_PROBE_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_DATASET_LOC
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_DATASET_LOC" FOR "DEAPP"."DE_SNP_DATA_DATASET_LOC";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_DATA_DS_LOC_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_DATA_DS_LOC_RELEASE" FOR "TM_CZ"."DE_SNP_DATA_DS_LOC_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_GENE_MAP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_GENE_MAP" FOR "DEAPP"."DE_SNP_GENE_MAP";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_GENE_MAP_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_GENE_MAP_RELEASE" FOR "TM_CZ"."DE_SNP_GENE_MAP_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_INFO
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_INFO" FOR "DEAPP"."DE_SNP_INFO";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_INFO_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_INFO_RELEASE" FOR "TM_CZ"."DE_SNP_INFO_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_PROBE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_PROBE" FOR "DEAPP"."DE_SNP_PROBE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_PROBE_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_PROBE_RELEASE" FOR "TM_CZ"."DE_SNP_PROBE_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_PROBE_SORTED_DEF
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_PROBE_SORTED_DEF" FOR "DEAPP"."DE_SNP_PROBE_SORTED_DEF";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_PROBE_SORT_DEF_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_PROBE_SORT_DEF_RELEASE" FOR "TM_CZ"."DE_SNP_PROBE_SORT_DEF_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SNP_SUBJECT_SORTED_DEF
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SNP_SUBJECT_SORTED_DEF" FOR "DEAPP"."DE_SNP_SUBJECT_SORTED_DEF";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_MICROARRAY_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_MICROARRAY_DATA" FOR "DEAPP"."DE_SUBJECT_MICROARRAY_DATA";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_MICROARRAY_DATA_OLD
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_MICROARRAY_DATA_OLD" FOR "DEAPP"."DE_SUBJECT_MICROARRAY_DATA_OLD";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_MICROARRAY_LOGS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_MICROARRAY_LOGS" FOR "DEAPP"."DE_SUBJECT_MICROARRAY_LOGS";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_MICROARRAY_MED
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_MICROARRAY_MED" FOR "DEAPP"."DE_SUBJECT_MICROARRAY_MED";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_MRNA_DATA_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_MRNA_DATA_RELEASE" FOR "TM_CZ"."DE_SUBJECT_MRNA_DATA_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_PROTEIN_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_PROTEIN_DATA" FOR "DEAPP"."DE_SUBJECT_PROTEIN_DATA";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_RBM_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_RBM_DATA" FOR "DEAPP"."DE_SUBJECT_RBM_DATA";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_RBM_DATA_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_RBM_DATA_RELEASE" FOR "TM_CZ"."DE_SUBJECT_RBM_DATA_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_SAMPLE_MAPPING
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_SAMPLE_MAPPING" FOR "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJECT_SNP_DATASET
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJECT_SNP_DATASET" FOR "DEAPP"."DE_SUBJECT_SNP_DATASET";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJ_PROTEIN_DATA_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJ_PROTEIN_DATA_RELEASE" FOR "TM_CZ"."DE_SUBJ_PROTEIN_DATA_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_SUBJ_SAMPLE_MAP_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_SUBJ_SAMPLE_MAP_RELEASE" FOR "TM_CZ"."DE_SUBJ_SAMPLE_MAP_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn DE_XTRIAL_CHILD_MAP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_XTRIAL_CHILD_MAP" FOR "DEAPP"."DE_XTRIAL_CHILD_MAP";
--------------------------------------------------------
--  DDL for Synonymn DE_XTRIAL_PARENT_NAMES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DE_XTRIAL_PARENT_NAMES" FOR "DEAPP"."DE_XTRIAL_PARENT_NAMES";
--------------------------------------------------------
--  DDL for Synonymn DIMLOADER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DIMLOADER" FOR "I2B2DEMODATA"."DIMLOADER";
--------------------------------------------------------
--  DDL for Synonymn DUMP_TABLE_TO_CSV
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DUMP_TABLE_TO_CSV" FOR "DEAPP"."DUMP_TABLE_TO_CSV";
--------------------------------------------------------
--  DDL for Synonymn DX
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."DX" FOR "I2B2DEMODATA"."DX";
--------------------------------------------------------
--  DDL for Synonymn ENCOUNTER_MAPPING
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ENCOUNTER_MAPPING" FOR "I2B2DEMODATA"."ENCOUNTER_MAPPING";
--------------------------------------------------------
--  DDL for Synonymn FACETED_SEARCH
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FACETED_SEARCH" FOR "BIOMART"."FACETED_SEARCH";
--------------------------------------------------------
--  DDL for Synonymn FM_DATA_UID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FM_DATA_UID" FOR "FMAPP"."FM_DATA_UID";
--------------------------------------------------------
--  DDL for Synonymn FM_FILE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FM_FILE" FOR "FMAPP"."FM_FILE";
--------------------------------------------------------
--  DDL for Synonymn FM_FOLDER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FM_FOLDER" FOR "FMAPP"."FM_FOLDER";
--------------------------------------------------------
--  DDL for Synonymn FM_FOLDER_ASSOCIATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FM_FOLDER_ASSOCIATION" FOR "FMAPP"."FM_FOLDER_ASSOCIATION";
--------------------------------------------------------
--  DDL for Synonymn FM_FOLDER_FILE_ASSOCIATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."FM_FOLDER_FILE_ASSOCIATION" FOR "FMAPP"."FM_FOLDER_FILE_ASSOCIATION";
--------------------------------------------------------
--  DDL for Synonymn GLOBAL_TEMP_FACT_PARAM_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."GLOBAL_TEMP_FACT_PARAM_TABLE" FOR "I2B2DEMODATA"."GLOBAL_TEMP_FACT_PARAM_TABLE";
--------------------------------------------------------
--  DDL for Synonymn GLOBAL_TEMP_PARAM_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."GLOBAL_TEMP_PARAM_TABLE" FOR "I2B2DEMODATA"."GLOBAL_TEMP_PARAM_TABLE";
--------------------------------------------------------
--  DDL for Synonymn HAPLOVIEW_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."HAPLOVIEW_DATA" FOR "DEAPP"."HAPLOVIEW_DATA";
--------------------------------------------------------
--  DDL for Synonymn HAPLOVIEW_DATA_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."HAPLOVIEW_DATA_RELEASE" FOR "TM_CZ"."HAPLOVIEW_DATA_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn HIBERNATE_SEQUENCE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."HIBERNATE_SEQUENCE" FOR "SEARCHAPP"."HIBERNATE_SEQUENCE";
--------------------------------------------------------
--  DDL for Synonymn HILOSEQUENCES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."HILOSEQUENCES" FOR "I2B2HIVE"."HILOSEQUENCES";
--------------------------------------------------------
--  DDL for Synonymn I2B2
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2" FOR "I2B2METADATA"."I2B2";
--------------------------------------------------------
--  DDL for Synonymn I2B2_ID_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_ID_SEQ" FOR "I2B2METADATA"."I2B2_ID_SEQ";
--------------------------------------------------------
--  DDL for Synonymn I2B2_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_RELEASE" FOR "TM_CZ"."I2B2_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn I2B2_SAMPLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_SAMPLE" FOR "I2B2SAMPLEDATA"."I2B2_SAMPLE";
--------------------------------------------------------
--  DDL for Synonymn I2B2_SECURE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_SECURE" FOR "I2B2METADATA"."I2B2_SECURE";
--------------------------------------------------------
--  DDL for Synonymn I2B2_TAGS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_TAGS" FOR "I2B2METADATA"."I2B2_TAGS";
--------------------------------------------------------
--  DDL for Synonymn I2B2_TAGS_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."I2B2_TAGS_RELEASE" FOR "TM_CZ"."I2B2_TAGS_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn INSERT_CONCEPT_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_CONCEPT_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_CONCEPT_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn INSERT_EID_MAP_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_EID_MAP_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_EID_MAP_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn INSERT_ENCOUNTERVISIT_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_ENCOUNTERVISIT_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_ENCOUNTERVISIT_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn INSERT_PATIENT_MAP_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_PATIENT_MAP_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_PATIENT_MAP_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn INSERT_PID_MAP_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_PID_MAP_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_PID_MAP_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn INSERT_PROVIDER_FROMTEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."INSERT_PROVIDER_FROMTEMP" FOR "I2B2DEMODATA"."INSERT_PROVIDER_FROMTEMP";
--------------------------------------------------------
--  DDL for Synonymn JMS_MESSAGES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."JMS_MESSAGES" FOR "I2B2HIVE"."JMS_MESSAGES";
--------------------------------------------------------
--  DDL for Synonymn JMS_ROLES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."JMS_ROLES" FOR "I2B2HIVE"."JMS_ROLES";
--------------------------------------------------------
--  DDL for Synonymn JMS_SUBSCRIPTIONS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."JMS_SUBSCRIPTIONS" FOR "I2B2HIVE"."JMS_SUBSCRIPTIONS";
--------------------------------------------------------
--  DDL for Synonymn JMS_TRANSACTIONS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."JMS_TRANSACTIONS" FOR "I2B2HIVE"."JMS_TRANSACTIONS";
--------------------------------------------------------
--  DDL for Synonymn JMS_USERS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."JMS_USERS" FOR "I2B2HIVE"."JMS_USERS";
--------------------------------------------------------
--  DDL for Synonymn LZ_SRC_ANALYSIS_METADATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."LZ_SRC_ANALYSIS_METADATA" FOR "TM_LZ"."LZ_SRC_ANALYSIS_METADATA";
--------------------------------------------------------
--  DDL for Synonymn LZ_SRC_STUDY_METADATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."LZ_SRC_STUDY_METADATA" FOR "TM_LZ"."LZ_SRC_STUDY_METADATA";
--------------------------------------------------------
--  DDL for Synonymn MASTER_QUERY_GLOBAL_TEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MASTER_QUERY_GLOBAL_TEMP" FOR "I2B2DEMODATA"."MASTER_QUERY_GLOBAL_TEMP";
--------------------------------------------------------
--  DDL for Synonymn MESH
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MESH" FOR "BIOMART"."MESH";
--------------------------------------------------------
--  DDL for Synonymn MESH_ALL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MESH_ALL" FOR "BIOMART"."MESH_ALL";
--------------------------------------------------------
--  DDL for Synonymn MESH_ENTRY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MESH_ENTRY" FOR "BIOMART"."MESH_ENTRY";
--------------------------------------------------------
--  DDL for Synonymn MESH_ENTRY_ALL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MESH_ENTRY_ALL" FOR "BIOMART"."MESH_ENTRY_ALL";
--------------------------------------------------------
--  DDL for Synonymn MLOG$_SEARCH_GENE_SIGNATUR
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."MLOG$_SEARCH_GENE_SIGNATUR" FOR "SEARCHAPP"."MLOG$_SEARCH_GENE_SIGNATUR";
--------------------------------------------------------
--  DDL for Synonymn NEWS_UPDATES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."NEWS_UPDATES" FOR "I2B2DEMODATA"."NEWS_UPDATES";
--------------------------------------------------------
--  DDL for Synonymn NODE_CURATION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."NODE_CURATION" FOR "TM_CZ"."NODE_CURATION";
--------------------------------------------------------
--  DDL for Synonymn OBSERVATION_FACT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."OBSERVATION_FACT" FOR "I2B2DEMODATA"."OBSERVATION_FACT";
--------------------------------------------------------
--  DDL for Synonymn OBSERVATION_FACT_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."OBSERVATION_FACT_RELEASE" FOR "TM_CZ"."OBSERVATION_FACT_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn ONT_DB_LOOKUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ONT_DB_LOOKUP" FOR "I2B2METADATA"."ONT_DB_LOOKUP";
--------------------------------------------------------
--  DDL for Synonymn ONT_PROCESS_STATUS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ONT_PROCESS_STATUS" FOR "I2B2METADATA"."ONT_PROCESS_STATUS";
--------------------------------------------------------
--  DDL for Synonymn ONT_SQ_PS_PRID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."ONT_SQ_PS_PRID" FOR "I2B2METADATA"."ONT_SQ_PS_PRID";
--------------------------------------------------------
--  DDL for Synonymn PATIENT_DIMENSION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PATIENT_DIMENSION" FOR "I2B2DEMODATA"."PATIENT_DIMENSION";
--------------------------------------------------------
--  DDL for Synonymn PATIENT_DIMENSION_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PATIENT_DIMENSION_RELEASE" FOR "TM_CZ"."PATIENT_DIMENSION_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn PATIENT_MAPPING
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PATIENT_MAPPING" FOR "I2B2DEMODATA"."PATIENT_MAPPING";
--------------------------------------------------------
--  DDL for Synonymn PATIENT_TRIAL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PATIENT_TRIAL" FOR "I2B2DEMODATA"."PATIENT_TRIAL";
--------------------------------------------------------
--  DDL for Synonymn PLUGIN
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PLUGIN" FOR "SEARCHAPP"."PLUGIN";
--------------------------------------------------------
--  DDL for Synonymn PLUGIN_MODULE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PLUGIN_MODULE" FOR "SEARCHAPP"."PLUGIN_MODULE";
--------------------------------------------------------
--  DDL for Synonymn PLUGIN_MODULE_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PLUGIN_MODULE_SEQ" FOR "SEARCHAPP"."PLUGIN_MODULE_SEQ";
--------------------------------------------------------
--  DDL for Synonymn PLUGIN_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PLUGIN_SEQ" FOR "SEARCHAPP"."PLUGIN_SEQ";
--------------------------------------------------------
--  DDL for Synonymn PROBESET_DEAPP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PROBESET_DEAPP" FOR "TM_CZ"."PROBESET_DEAPP";
--------------------------------------------------------
--  DDL for Synonymn PROBESET_DEAPP_20120206
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PROBESET_DEAPP_20120206" FOR "TM_CZ"."PROBESET_DEAPP_20120206";
--------------------------------------------------------
--  DDL for Synonymn PROJECT_INFO
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PROJECT_INFO" FOR "BIOMART"."PROJECT_INFO";
--------------------------------------------------------
--  DDL for Synonymn PROTOCOL_ID_SEQ
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PROTOCOL_ID_SEQ" FOR "I2B2DEMODATA"."PROTOCOL_ID_SEQ";
--------------------------------------------------------
--  DDL for Synonymn PROVIDER_DIMENSION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."PROVIDER_DIMENSION" FOR "I2B2DEMODATA"."PROVIDER_DIMENSION";
--------------------------------------------------------
--  DDL for Synonymn QT_ANALYSIS_PLUGIN
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_ANALYSIS_PLUGIN" FOR "I2B2DEMODATA"."QT_ANALYSIS_PLUGIN";
--------------------------------------------------------
--  DDL for Synonymn QT_ANALYSIS_PLUGIN_RESULT_TYPE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_ANALYSIS_PLUGIN_RESULT_TYPE" FOR "I2B2DEMODATA"."QT_ANALYSIS_PLUGIN_RESULT_TYPE";
--------------------------------------------------------
--  DDL for Synonymn QT_BREAKDOWN_PATH
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_BREAKDOWN_PATH" FOR "I2B2DEMODATA"."QT_BREAKDOWN_PATH";
--------------------------------------------------------
--  DDL for Synonymn QT_PATIENT_ENC_COLLECTION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_PATIENT_ENC_COLLECTION" FOR "I2B2DEMODATA"."QT_PATIENT_ENC_COLLECTION";
--------------------------------------------------------
--  DDL for Synonymn QT_PATIENT_SET_COLLECTION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_PATIENT_SET_COLLECTION" FOR "I2B2DEMODATA"."QT_PATIENT_SET_COLLECTION";
--------------------------------------------------------
--  DDL for Synonymn QT_PDO_QUERY_MASTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_PDO_QUERY_MASTER" FOR "I2B2DEMODATA"."QT_PDO_QUERY_MASTER";
--------------------------------------------------------
--  DDL for Synonymn QT_PRIVILEGE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_PRIVILEGE" FOR "I2B2DEMODATA"."QT_PRIVILEGE";
--------------------------------------------------------
--  DDL for Synonymn QT_QUERY_INSTANCE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_QUERY_INSTANCE" FOR "I2B2DEMODATA"."QT_QUERY_INSTANCE";
--------------------------------------------------------
--  DDL for Synonymn QT_QUERY_MASTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_QUERY_MASTER" FOR "I2B2DEMODATA"."QT_QUERY_MASTER";
--------------------------------------------------------
--  DDL for Synonymn QT_QUERY_RESULT_INSTANCE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_QUERY_RESULT_INSTANCE" FOR "I2B2DEMODATA"."QT_QUERY_RESULT_INSTANCE";
--------------------------------------------------------
--  DDL for Synonymn QT_QUERY_RESULT_TYPE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_QUERY_RESULT_TYPE" FOR "I2B2DEMODATA"."QT_QUERY_RESULT_TYPE";
--------------------------------------------------------
--  DDL for Synonymn QT_QUERY_STATUS_TYPE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_QUERY_STATUS_TYPE" FOR "I2B2DEMODATA"."QT_QUERY_STATUS_TYPE";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_PQM_QMID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_PQM_QMID" FOR "I2B2DEMODATA"."QT_SQ_PQM_QMID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QI_QIID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QI_QIID" FOR "I2B2DEMODATA"."QT_SQ_QI_QIID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QM_QMID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QM_QMID" FOR "I2B2DEMODATA"."QT_SQ_QM_QMID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QPER_PECID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QPER_PECID" FOR "I2B2DEMODATA"."QT_SQ_QPER_PECID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QPR_PCID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QPR_PCID" FOR "I2B2DEMODATA"."QT_SQ_QPR_PCID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QRI_QRIID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QRI_QRIID" FOR "I2B2DEMODATA"."QT_SQ_QRI_QRIID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QR_QRID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QR_QRID" FOR "I2B2DEMODATA"."QT_SQ_QR_QRID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QS_QSID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QS_QSID" FOR "I2B2DEMODATA"."QT_SQ_QS_QSID";
--------------------------------------------------------
--  DDL for Synonymn QT_SQ_QXR_XRID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_SQ_QXR_XRID" FOR "I2B2DEMODATA"."QT_SQ_QXR_XRID";
--------------------------------------------------------
--  DDL for Synonymn QT_XML_RESULT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QT_XML_RESULT" FOR "I2B2DEMODATA"."QT_XML_RESULT";
--------------------------------------------------------
--  DDL for Synonymn QUERY1
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QUERY1" FOR "I2B2METADATA"."QUERY1";
--------------------------------------------------------
--  DDL for Synonymn QUERY_GLOBAL_TEMP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."QUERY_GLOBAL_TEMP" FOR "I2B2DEMODATA"."QUERY_GLOBAL_TEMP";
--------------------------------------------------------
--  DDL for Synonymn REMOVE_TEMP_TABLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."REMOVE_TEMP_TABLE" FOR "I2B2DEMODATA"."REMOVE_TEMP_TABLE";
--------------------------------------------------------
--  DDL for Synonymn REPORT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."REPORT" FOR "SEARCHAPP"."REPORT";
--------------------------------------------------------
--  DDL for Synonymn REPORT_ITEM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."REPORT_ITEM" FOR "SEARCHAPP"."REPORT_ITEM";
--------------------------------------------------------
--  DDL for Synonymn SAMPLE_CATEGORIES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SAMPLE_CATEGORIES" FOR "I2B2DEMODATA"."SAMPLE_CATEGORIES";
--------------------------------------------------------
--  DDL for Synonymn SAMPLE_CATEGORIES_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SAMPLE_CATEGORIES_RELEASE" FOR "TM_CZ"."SAMPLE_CATEGORIES_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn SCHEMES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SCHEMES" FOR "I2B2METADATA"."SCHEMES";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_APP_ACCESS_LOG
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_APP_ACCESS_LOG" FOR "SEARCHAPP"."SEARCH_APP_ACCESS_LOG";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_GROUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_GROUP" FOR "SEARCHAPP"."SEARCH_AUTH_GROUP";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_GROUP_MEMBER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_GROUP_MEMBER" FOR "SEARCHAPP"."SEARCH_AUTH_GROUP_MEMBER";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_PRINCIPAL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_PRINCIPAL" FOR "SEARCHAPP"."SEARCH_AUTH_PRINCIPAL";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_SEC_OBJECT_ACCESS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_SEC_OBJECT_ACCESS" FOR "SEARCHAPP"."SEARCH_AUTH_SEC_OBJECT_ACCESS";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_USER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_USER" FOR "SEARCHAPP"."SEARCH_AUTH_USER";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_USER_SEC_ACCESS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_USER_SEC_ACCESS" FOR "SEARCHAPP"."SEARCH_AUTH_USER_SEC_ACCESS";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_AUTH_USER_SEC_ACCESS_V
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_AUTH_USER_SEC_ACCESS_V" FOR "SEARCHAPP"."SEARCH_AUTH_USER_SEC_ACCESS_V";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_BIO_MKR_CORREL_FAST_MV
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_BIO_MKR_CORREL_FAST_MV" FOR "SEARCHAPP"."SEARCH_BIO_MKR_CORREL_FAST_MV";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_BIO_MKR_CORREL_VIEW
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_BIO_MKR_CORREL_VIEW" FOR "SEARCHAPP"."SEARCH_BIO_MKR_CORREL_VIEW";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_CUSTOM_FILTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_CUSTOM_FILTER" FOR "SEARCHAPP"."SEARCH_CUSTOM_FILTER";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_CUSTOM_FILTER_ITEM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_CUSTOM_FILTER_ITEM" FOR "SEARCHAPP"."SEARCH_CUSTOM_FILTER_ITEM";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_GENE_SIGNATURE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_GENE_SIGNATURE" FOR "SEARCHAPP"."SEARCH_GENE_SIGNATURE";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_GENE_SIGNATURE_ITEM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_GENE_SIGNATURE_ITEM" FOR "SEARCHAPP"."SEARCH_GENE_SIGNATURE_ITEM";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_GENE_SIG_FILE_SCHEMA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_GENE_SIG_FILE_SCHEMA" FOR "SEARCHAPP"."SEARCH_GENE_SIG_FILE_SCHEMA";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_KEYWORD
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_KEYWORD" FOR "SEARCHAPP"."SEARCH_KEYWORD";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_KEYWORD1
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_KEYWORD1" FOR "SEARCHAPP"."SEARCH_KEYWORD1";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_KEYWORD_TERM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_KEYWORD_TERM" FOR "SEARCHAPP"."SEARCH_KEYWORD_TERM";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_KEYWORD_TERM1
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_KEYWORD_TERM1" FOR "SEARCHAPP"."SEARCH_KEYWORD_TERM1";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_REQUEST_MAP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_REQUEST_MAP" FOR "SEARCHAPP"."SEARCH_REQUEST_MAP";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_ROLE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_ROLE" FOR "SEARCHAPP"."SEARCH_ROLE";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_ROLE_AUTH_USER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_ROLE_AUTH_USER" FOR "SEARCHAPP"."SEARCH_ROLE_AUTH_USER";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_SECURE_OBJECT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_SECURE_OBJECT" FOR "SEARCHAPP"."SEARCH_SECURE_OBJECT";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_SECURE_OBJECT_PATH
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_SECURE_OBJECT_PATH" FOR "SEARCHAPP"."SEARCH_SECURE_OBJECT_PATH";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_SECURE_OBJECT_RELEASE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_SECURE_OBJECT_RELEASE" FOR "TM_CZ"."SEARCH_SECURE_OBJECT_RELEASE";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_SEC_ACCESS_LEVEL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_SEC_ACCESS_LEVEL" FOR "SEARCHAPP"."SEARCH_SEC_ACCESS_LEVEL";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_TAXONOMY
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_TAXONOMY" FOR "SEARCHAPP"."SEARCH_TAXONOMY";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_TAXONOMY_RELS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_TAXONOMY_RELS" FOR "SEARCHAPP"."SEARCH_TAXONOMY_RELS";
--------------------------------------------------------
--  DDL for Synonymn SEARCH_USER_FEEDBACK
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEARCH_USER_FEEDBACK" FOR "SEARCHAPP"."SEARCH_USER_FEEDBACK";
--------------------------------------------------------
--  DDL for Synonymn SEQ_ASSAY_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_ASSAY_ID" FOR "DEAPP"."SEQ_ASSAY_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_BIO_DATA_FACT_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_BIO_DATA_FACT_ID" FOR "BIOMART"."SEQ_BIO_DATA_FACT_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_BIO_DATA_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_BIO_DATA_ID" FOR "BIOMART"."SEQ_BIO_DATA_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_CLINICAL_TRIAL_DESIGN_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_CLINICAL_TRIAL_DESIGN_ID" FOR "BIOMART"."SEQ_CLINICAL_TRIAL_DESIGN_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_CONCEPT_CODE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_CONCEPT_CODE" FOR "I2B2METADATA"."SEQ_CONCEPT_CODE";
--------------------------------------------------------
--  DDL for Synonymn SEQ_DATA_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_DATA_ID" FOR "DEAPP"."SEQ_DATA_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_ETL_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_ETL_ID" FOR "TM_LZ"."SEQ_ETL_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_FM_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_FM_ID" FOR "FMAPP"."SEQ_FM_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_I2B2_DATA_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_I2B2_DATA_ID" FOR "I2B2METADATA"."SEQ_I2B2_DATA_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_PATIENT_NUM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_PATIENT_NUM" FOR "I2B2DEMODATA"."SEQ_PATIENT_NUM";
--------------------------------------------------------
--  DDL for Synonymn SEQ_SEARCH_DATA_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SEARCH_DATA_ID" FOR "SEARCHAPP"."SEQ_SEARCH_DATA_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_SEARCH_TAXONOMY_RELS_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SEARCH_TAXONOMY_RELS_ID" FOR "SEARCHAPP"."SEQ_SEARCH_TAXONOMY_RELS_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_SEARCH_TAXONOMY_TERM_ID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SEARCH_TAXONOMY_TERM_ID" FOR "SEARCHAPP"."SEQ_SEARCH_TAXONOMY_TERM_ID";
--------------------------------------------------------
--  DDL for Synonymn SEQ_SUBJECT_REFERENCE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SEQ_SUBJECT_REFERENCE" FOR "I2B2DEMODATA"."SEQ_SUBJECT_REFERENCE";
--------------------------------------------------------
--  DDL for Synonymn SETONT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SETONT" FOR "I2B2DEMODATA"."SETONT";
--------------------------------------------------------
--  DDL for Synonymn SET_TYPE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SET_TYPE" FOR "I2B2DEMODATA"."SET_TYPE";
--------------------------------------------------------
--  DDL for Synonymn SET_UPLOAD_STATUS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SET_UPLOAD_STATUS" FOR "I2B2DEMODATA"."SET_UPLOAD_STATUS";
--------------------------------------------------------
--  DDL for Synonymn SOURCE_MASTER
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SOURCE_MASTER" FOR "I2B2DEMODATA"."SOURCE_MASTER";
--------------------------------------------------------
--  DDL for Synonymn SP_XTAB
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SP_XTAB" FOR "I2B2DEMODATA"."SP_XTAB";
--------------------------------------------------------
--  DDL for Synonymn SQ_UPLOADSTATUS_UPLOADID
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SQ_UPLOADSTATUS_UPLOADID" FOR "I2B2DEMODATA"."SQ_UPLOADSTATUS_UPLOADID";
--------------------------------------------------------
--  DDL for Synonymn SQ_UP_ENCDIM_ENCOUNTERNUM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SQ_UP_ENCDIM_ENCOUNTERNUM" FOR "I2B2DEMODATA"."SQ_UP_ENCDIM_ENCOUNTERNUM";
--------------------------------------------------------
--  DDL for Synonymn SQ_UP_PATDIM_PATIENTNUM
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SQ_UP_PATDIM_PATIENTNUM" FOR "I2B2DEMODATA"."SQ_UP_PATDIM_PATIENTNUM";
--------------------------------------------------------
--  DDL for Synonymn SUBSET
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."SUBSET" FOR "SEARCHAPP"."SUBSET";
--------------------------------------------------------
--  DDL for Synonymn T1
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."T1" FOR "BIOMART"."T1";
--------------------------------------------------------
--  DDL for Synonymn TABLE_ACCESS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TABLE_ACCESS" FOR "I2B2METADATA"."TABLE_ACCESS";
--------------------------------------------------------
--  DDL for Synonymn TIMERS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TIMERS" FOR "I2B2HIVE"."TIMERS";
--------------------------------------------------------
--  DDL for Synonymn TJEA_JUNK
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TJEA_JUNK" FOR "TM_CZ"."TJEA_JUNK";
--------------------------------------------------------
--  DDL for Synonymn TMP_ANALYSIS_DATA_TEA_RANK
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TMP_ANALYSIS_DATA_TEA_RANK" FOR "BIOMART"."TMP_ANALYSIS_DATA_TEA_RANK";
--------------------------------------------------------
--  DDL for Synonymn TMP_NUM_DATA_TYPES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TMP_NUM_DATA_TYPES" FOR "TM_CZ"."TMP_NUM_DATA_TYPES";
--------------------------------------------------------
--  DDL for Synonymn TMP_SUBJECT_INFO
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TMP_SUBJECT_INFO" FOR "TM_CZ"."TMP_SUBJECT_INFO";
--------------------------------------------------------
--  DDL for Synonymn TMP_TRIAL_DATA
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TMP_TRIAL_DATA" FOR "TM_CZ"."TMP_TRIAL_DATA";
--------------------------------------------------------
--  DDL for Synonymn TMP_TRIAL_NODES
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."TMP_TRIAL_NODES" FOR "TM_CZ"."TMP_TRIAL_NODES";
--------------------------------------------------------
--  DDL for Synonymn UPDATE_OBSERVATION_FACT
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."UPDATE_OBSERVATION_FACT" FOR "I2B2DEMODATA"."UPDATE_OBSERVATION_FACT";
--------------------------------------------------------
--  DDL for Synonymn UPLOAD_STATUS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."UPLOAD_STATUS" FOR "I2B2DEMODATA"."UPLOAD_STATUS";
--------------------------------------------------------
--  DDL for Synonymn UTIL_GRANT_ALL
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."UTIL_GRANT_ALL" FOR "DEAPP"."UTIL_GRANT_ALL";
--------------------------------------------------------
--  DDL for Synonymn VISIT_DIMENSION
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."VISIT_DIMENSION" FOR "I2B2DEMODATA"."VISIT_DIMENSION";
--------------------------------------------------------
--  DDL for Synonymn VW_FACETED_SEARCH
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."VW_FACETED_SEARCH" FOR "BIOMART"."VW_FACETED_SEARCH";
--------------------------------------------------------
--  DDL for Synonymn WORKPLACE
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."WORKPLACE" FOR "I2B2WORKDATA"."WORKPLACE";
--------------------------------------------------------
--  DDL for Synonymn WORKPLACE_ACCESS
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."WORKPLACE_ACCESS" FOR "I2B2WORKDATA"."WORKPLACE_ACCESS";
--------------------------------------------------------
--  DDL for Synonymn WORK_DB_LOOKUP
--------------------------------------------------------

  CREATE OR REPLACE SYNONYM "BIOMART_USER"."WORK_DB_LOOKUP" FOR "I2B2HIVE"."WORK_DB_LOOKUP";
