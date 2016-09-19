--
-- Name: batch_step_execution_context; Type: TABLE; Schema: ts_batch; Owner: -
--
CREATE TABLE batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);

--
-- Name: batch_step_execution_context_pkey; Type: CONSTRAINT; Schema: ts_batch; Owner: -
--
ALTER TABLE ONLY batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);

--
-- Name: step_exec_ctx_fk; Type: FK CONSTRAINT; Schema: ts_batch; Owner: -
--
ALTER TABLE ONLY batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES batch_step_execution(step_execution_id);

