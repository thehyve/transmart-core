--
-- Name: async_job; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE async_job (
    id integer NOT NULL,
    job_name character varying(200),
    job_status character varying(200),
    run_time character varying(200),
    job_status_time timestamp(6) without time zone,
    last_run_on timestamp(6) without time zone,
    viewer_url character varying(4000),
    alt_viewer_url character varying(4000),
    job_results text,
    job_inputs_json text,
    job_type character varying(20),
    user_id character varying(50)
);

--
-- Name: async_job_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY async_job
    ADD CONSTRAINT async_job_pkey PRIMARY KEY (id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.async_job IS 'Stores record per an advanced analysis job.';

COMMENT ON COLUMN async_job.job_name IS 'The job name. e.g. admin-RHeatmap-100448';
COMMENT ON COLUMN async_job.job_status IS 'The status of the job. [Started|Cancelled|Error|Completed|...]';
COMMENT ON COLUMN async_job.job_status_time IS 'The time when current job_status took place.';
COMMENT ON COLUMN async_job.last_run_on IS 'The start time of the job.';
COMMENT ON COLUMN async_job.user_id IS 'Name of the user.';
