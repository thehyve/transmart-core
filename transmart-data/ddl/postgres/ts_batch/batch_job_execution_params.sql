--
-- Name: batch_job_execution_params; Type: TABLE; Schema: ts_batch; Owner: -
--
CREATE TABLE batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    type_cd character varying(6) NOT NULL,
    key_name character varying(100) NOT NULL,
    string_val character varying(250),
    date_val timestamp without time zone,
    long_val bigint,
    double_val double precision,
    identifying character(1) NOT NULL
);

--
-- Name: job_exec_params_fk; Type: FK CONSTRAINT; Schema: ts_batch; Owner: -
--
ALTER TABLE ONLY batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id);

