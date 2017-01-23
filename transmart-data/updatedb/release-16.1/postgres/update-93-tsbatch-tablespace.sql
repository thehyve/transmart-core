--
-- fix owner, tablespaces and permissions for ts_batch
-- handled automatically in initial make, processed individually here
--

set search_path = ts_batch, pg_catalog;

ALTER SCHEMA ts_batch OWNER TO ts_batch;

ALTER TABLE batch_job_execution OWNER TO ts_batch, SET TABLESPACE transmart;
ALTER TABLE batch_job_execution_context OWNER TO ts_batch, SET TABLESPACE transmart;
ALTER TABLE batch_job_execution_params OWNER TO ts_batch, SET TABLESPACE transmart;
ALTER TABLE batch_job_instance OWNER TO ts_batch, SET TABLESPACE transmart;
ALTER TABLE batch_step_execution OWNER TO ts_batch, SET TABLESPACE transmart;
ALTER TABLE batch_step_execution_context OWNER TO ts_batch, SET TABLESPACE transmart;

ALTER INDEX batch_job_execution_pkey SET TABLESPACE indx;
ALTER INDEX batch_job_execution_context_pkey SET TABLESPACE indx;
ALTER INDEX batch_job_instance_pkey SET TABLESPACE indx;
ALTER INDEX batch_step_execution_pkey SET TABLESPACE indx;
ALTER INDEX batch_step_execution_context_pkey SET TABLESPACE indx;
ALTER INDEX job_inst_un SET TABLESPACE indx;

ALTER SEQUENCE batch_job_execution_seq OWNER TO ts_batch;
ALTER SEQUENCE batch_job_seq OWNER TO ts_batch;
ALTER SEQUENCE batch_step_execution_seq OWNER TO ts_batch;

GRANT ALL ON TABLE batch_job_execution TO tm_cz;
GRANT ALL ON TABLE batch_job_execution TO tsbatch;
GRANT ALL ON TABLE batch_job_execution_context TO tm_cz;
GRANT ALL ON TABLE batch_job_execution_context TO tsbatch;
GRANT ALL ON TABLE batch_job_execution_params TO tm_cz;
GRANT ALL ON TABLE batch_job_execution_params TO tsbatch;
GRANT ALL ON TABLE batch_job_instance TO tm_cz;
GRANT ALL ON TABLE batch_job_instance TO tsbatch;
GRANT ALL ON TABLE batch_step_execution TO tm_cz;
GRANT ALL ON TABLE batch_step_execution TO tsbatch;
GRANT ALL ON TABLE batch_step_execution_context TO tm_cz;
GRANT ALL ON TABLE batch_step_execution_context TO tsbatch;
GRANT ALL ON TABLE batch_step_execution_context_pkey TO tm_cz;
GRANT ALL ON TABLE batch_step_execution_context_pkey TO tsbatch;
GRANT ALL ON TABLE job_inst_un TO tm_cz;
GRANT ALL ON TABLE job_inst_un TO tsbatch;

GRANT ALL ON SEQUENCE batch_job_execution_seq TO tm_cz;
GRANT ALL ON SEQUENCE batch_job_execution_seq TO tsbatch;
GRANT ALL ON SEQUENCE batch_job_seq TO tm_cz;
GRANT ALL ON SEQUENCE batch_job_seq TO tsbatch;
GRANT ALL ON SEQUENCE batch_step_execution_seq TO tm_cz;
GRANT ALL ON SEQUENCE batch_step_execution_seq TO tsbatch;

