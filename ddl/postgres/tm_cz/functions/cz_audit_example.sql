--
-- Name: cz_audit_example(bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION cz_audit_example(currentjobid bigint) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;

BEGIN
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

  PERFORM sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName ;
  procedureName := $$PLSQL_UNIT;

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    RAISE NOTICE '%%', 'Here' ,  to_char(jobID);
    cz_start_audit (procedureName, databaseName, jobID);
    RAISE NOTICE '%%', 'Here2' ,  to_char(jobID);
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
 
$_$;

