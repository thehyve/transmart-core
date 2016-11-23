--
-- Type: PROCEDURE; Owner: TM_CZ; Name: SET_BIO_DATA_UID_DIS
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."SET_BIO_DATA_UID_DIS" 
as
  --jobRunID CONTROL.SYSTEM_JOB_RUN.JOB_RUN_ID%TYPE;
  --jobStepID CONTROL.SYSTEM_JOB_STEP.JOB_STEP_ID%TYPE;
--CREATE or REPLACE  SYNONYM genego for pictor.genego;
BEGIN

-------------------------------------------------------------------------------
-- Loads data from PICTOR into biomart_LZ
--  emt@20090310
--------------------------------------------------------------------------------
--  jobrunid := control.insert_system_job_run('LoadGeneGOPathways', 'Load All Pathways from GENEGO in PICTOR');

begin

  --jobStepID := control.insert_system_job_step(jobRunID, 'Insert disease pathways into bio_marker for GENEGO disease pathways'
  --, 'Insert disease pathways into bio_marker for GENEGO disease pathways', 22);
  execute immediate 'delete from bio_data_uid where unique_id in
                    (select bio_disease_uid(mesh_code) from bio_disease)';
  execute immediate 'insert into bio_data_uid(
                    bio_data_id, unique_id, bio_data_type)
                    select
                    bio_disease_id, bio_disease_uid(mesh_code), ''BIO_DISEASE''
                    from bio_disease
                    where not exists
                      (select 1 from bio_data_uid
                      where bio_disease_uid(bio_disease.mesh_code) = bio_data_uid.unique_id)';

  --control.update_system_job_step_pass(jobStepID, SQL%ROWCOUNT);
  commit;

end;
/*
control.update_system_job_run_pass(jobRunID);
exception
when others then
  control.update_system_job_step_fail(jobStepID, sqlcode, sqlerrm()
  , dbms_utility.format_error_stack, dbms_utility.format_error_backtrace);
  control.update_system_job_run_fail(jobRunID);
  */
end;




/
 
