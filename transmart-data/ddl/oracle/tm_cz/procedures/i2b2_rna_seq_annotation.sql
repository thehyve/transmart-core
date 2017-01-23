--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_RNA_SEQ_ANNOTATION
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_RNA_SEQ_ANNOTATION" 
( rtn_code OUT NUMBER,
currentJobID NUMBER := null
) AS
gpl_rtn NUMBER;
missing_platform	exception;
newJobFlag INTEGER(1);
databaseName VARCHAR(100);
procedureName VARCHAR(100);
jobID number(18,0);
stepCt number(18,0);
BEGIN
 -- insert into "DEAPP"."DE_RNASEQ_ANNOTATION"
 stepCt := 0;

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		cz_start_audit (procedureName, databaseName, jobID);
	END IF;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_rna_seq_annotation',0,stepCt,'Done');

 insert into DE_RNASEQ_ANNOTATION 
 (
 TRANSCRIPT_ID 
   ,GPL_ID
    ,GENE_SYMBOL
  ,GENE_ID
  ,ORGANISM
 -- ,PROBESET_ID
    )
    select distinct (a.transcript_id)
      --,g.platform 
      ,null
          ,a.gene_symbol
          ,null--b.primary_external_id
          ,a.organism
         -- ,null
          --,pd.probeset_id
          from LT_RNASEQ_ANNOTATION a
              --,(select platform from de_gpl_info where marker_type='RNASEQ') g
              -- ,bio_marker b
             --  ,probeset_deapp pd
                where ---b.bio_marker_name=a.gene_symbol
               --and a.transcript_id =pd.probeset
              --  and 
                a.transcript_id not in (select distinct transcript_id from DE_RNASEQ_ANNOTATION);
                
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Insert data in DE_RNASEQ_ANNOTATION',0,stepCt,'Done');
                ---update gene_id from bio_marker  table
                
                
             update DE_RNASEQ_ANNOTATION a set GENE_ID=(select primary_external_id from bio_marker b where 
  b.bio_marker_name=a.gene_symbol and rownum=1)
                                where a.GENE_ID is null;
                
  stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_rna_seq_annotation',0,stepCt,'Done');
   IF newJobFlag = 1
  THEN
    cz_end_audit (jobID, 'SUCCESS');
  END IF;
  select 0 into rtn_code from dual;
               
               --update DE_RNASEQ_ANNOTATION 
          --     update DE_RNASEQ_ANNOTATION  set gene_id=
         --      ---Exceptions occur
               --
               EXCEPTION
               WHEN OTHERS THEN
		--Handle errors.
		--cz_error_handler (jobID, procedureName);
		--End Proc
		--cz_end_audit (jobID, 'FAIL');
		cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
    select 162  into rtn_code from dual;
 
END ;
/
 
