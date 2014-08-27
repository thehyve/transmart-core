--
-- Type: PROCEDURE; Owner: TM_CZ; Name: SET_BIO_DATA_UID_PATH
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."SET_BIO_DATA_UID_PATH" 
as
 -- jobRunID CONTROL.SYSTEM_JOB_RUN.JOB_RUN_ID%TYPE;
 -- jobStepID CONTROL.SYSTEM_JOB_STEP.JOB_STEP_ID%TYPE;
--CREATE or REPLACE  SYNONYM genego for pictor.genego;
BEGIN

-------------------------------------------------------------------------------
-- Loads data from PICTOR into biomart_LZ
--  emt@20090310
--------------------------------------------------------------------------------
  --jobrunid := control.insert_system_job_run('LoadGeneGOPathways', 'Load All Pathways from GENEGO in PICTOR');



  --jobStepID := control.insert_system_job_step(jobRunID, 'Insert disease pathways into bio_marker for GENEGO disease pathways'
  --, 'Insert disease pathways into bio_marker for GENEGO disease pathways', 22);
  execute immediate 'delete from bio_data_uid where unique_id in
                    (select biomarker_pathway_uid(primary_source_code, primary_external_id)
                    from bio_marker)';
  execute immediate 'delete from bio_data_uid where unique_id in
                    (select biomarker_gene_uid(primary_external_id)
                    from bio_marker)';
  execute immediate 'insert into bio_data_uid(
                    bio_data_id, unique_id, bio_data_type)
                    select
                    bio_marker_id
                    , biomarker_pathway_uid(primary_source_code, primary_external_id)
                    , ''BIO_MARKER.PATHWAY''
                    from bio_marker
                    where bio_marker_type=''PATHWAY''
                    and not exists
                      (select 1 from bio_data_uid
                      where biomarker_pathway_uid(bio_marker.primary_source_code, bio_marker.primary_external_id) =
                      bio_data_uid.unique_id)';
  execute immediate 'insert into bio_data_uid(
                    bio_data_id, unique_id, bio_data_type)
                    select
                    bio_marker_id
                    , biomarker_gene_uid(primary_external_id)
                    , ''BIO_MARKER.GENE''
                    from bio_marker
                    where bio_marker_type=''GENE''
                    and not exists
                      (select 1 from bio_data_uid
                      where biomarker_gene_uid(bio_marker.primary_external_id) = bio_data_uid.unique_id)';


  --control.update_system_job_step_pass(jobStepID, SQL%ROWCOUNT);
  commit;


--control.update_system_job_run_pass(jobRunID);
--exception
--when others then
  --control.update_system_job_step_fail(jobStepID, sqlcode, sqlerrm()
  --, dbms_utility.format_error_stack, dbms_utility.format_error_backtrace);
  --control.update_system_job_run_fail(jobRunID);
end;




/
 
