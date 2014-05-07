CREATE OR REPLACE PROCEDURE TM_CZ."I2B2_RENAME_GWAS_ANALYSIS" 
(
  study_id varchar2,
  old_name VARCHAR2,
  new_name VARCHAR2,
  currentJobID number:=null
)
AS

   
  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);
  b_analysis_id number(18,0);
  etlid number(18,0);
  pExists        int;
  data_type VARCHAR(50);
  
  MULTIPLE_ANALYSES exception;
  ANALYSES_NOTFOUND exception;
  
BEGIN
 -------------------------------------------------------------
  --Rename the analysis name for a GWAS analyses
  -- HZ@20140128 - First Rev
  -------------------------------------------------------------
    
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
        
  stepCt := 0;

  
    stepCt := stepCt + 1;
    cz_write_audit(jobId,databaseName,procedureName,'Start i2b2_rename_GWAS_analysis',0,stepCt,'Done'); 
    
  if old_name != ''  or old_name != '%' or new_name != ''  or new_name != '%'
  then

    -- get the analysis_id from biomart.bio_assay_analysis table
    select count(*) into pExists from  biomart.bio_assay_analysis WHERE analysis_name=old_name and ETL_ID=study_id;
    
    if pExists>1 then
      raise MULTIPLE_ANALYSES;
    elsif pExists =0 then
      raise ANALYSES_NOTFOUND;
    end if;
     
    select bio_assay_analysis_id, ETL_ID_SOURCE, bio_assay_data_type into b_analysis_id, etlid, data_type 
      from biomart.bio_assay_analysis WHERE analysis_name=old_name and ETL_ID=study_id;
   
    -- update bio_asy_analysis_gwas_top50 if GWAS
    if data_type='EQTL' then
        update biomart.bio_asy_analysis_eQTL_TOP50 set analysis=new_name where bio_assay_analysis_id=b_analysis_id;
    else 
        update biomart.bio_asy_analysis_GWAS_TOP50 set analysis=new_name where bio_assay_analysis_id=b_analysis_id;
    end if;   
    
    stepCt := stepCt + 1;
    cz_write_audit(jobId,databaseName,procedureName,'Update bio_asy_analysis_GWAS/eQTL_top50',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;
    
    --Update bio_assay_analysis table
    update biomart.bio_assay_analysis set analysis_name=new_name WHERE bio_assay_analysis_id=b_analysis_id;
    stepCt := stepCt + 1;
    cz_write_audit(jobId,databaseName,procedureName,'Update bio_assay_analysis ',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;
    
   
    --Update tm_lz.lz_src_analsis_metadata table
    update TM_LZ.LZ_SRC_ANALYSIS_METADATA 
      set ANALYSIS_NAME = new_name
      where ETL_ID=etlid;
    stepCt := stepCt + 1;
    cz_write_audit(jobId,databaseName,procedureName,'Update lz_src_analysis_metadata ',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;
  END IF;
  cz_write_audit(jobId,databaseName,procedureName,'End i2b2_rename_GWAS_analysis',0,stepCt,'Done');
  stepCt := stepCt + 1;

  cz_end_audit(jobId, 'Success');
  EXCEPTION
  when MULTIPLE_ANALYSES then
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Error: There are multiple analyses corresponding to '|| study_id || ':' || old_name,0,stepCt,'Done');    
        cz_error_handler (jobID, procedureName);
        cz_end_audit (jobID, 'FAIL');
  WHEN ANALYSES_NOTFOUND then
        stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Error: There is no analyses corresponding to '|| study_id || ':' || old_name,0,stepCt,'Done');    
        cz_error_handler (jobID, procedureName);
        cz_end_audit (jobID, 'FAIL');  
  WHEN OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
END;
/
