--
-- Name: batch_job_execution; Type: TABLE; Schema: ts_batch; Owner: -
--
CREATE TABLE batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500)
);

--
-- Name: batch_job_execution_pkey; Type: CONSTRAINT; Schema: ts_batch; Owner: -
--
ALTER TABLE ONLY batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);

--
-- Name: job_inst_exec_fk; Type: FK CONSTRAINT; Schema: ts_batch; Owner: -
--
ALTER TABLE ONLY batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES batch_job_instance(job_instance_id);

