--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_AUDIT_EXAMPLE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_AUDIT_EXAMPLE" 
(
  currentJobID NUMBER
)
AS
  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
BEGIN
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
    dbms_output.put_line('Here' || to_char(jobID));
    cz_start_audit (procedureName, databaseName, jobID);
    dbms_output.put_line('Here2' || to_char(jobID));
  END IF;

  --Step Audit
  cz_write_audit (jobID, databaseName, procedureName, 'Start loading some data', SQL%ROWCOUNT, 1, 'PASS');

  update cz_job_master set job_name = job_name;

  --Step Audit
  cz_write_audit (jobID, databaseName, procedureName, '# of rows on the cz_job_master table', SQL%ROWCOUNT, 2, 'PASS');


  cz_write_info (jobID, 1, 39, procedureName, 'Writing a message');



  --invalid statement
  insert into az_test_run(dw_version_id)
    values('a');


  --Step Audit
  cz_write_audit (jobID, databaseName, procedureName, 'Should have caused an error!', SQL%ROWCOUNT, 3, 'PASS');


  ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    cz_end_audit (jobID, 'SUCCESS');
  END IF;

  EXCEPTION
  WHEN OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');

END;



/
 
