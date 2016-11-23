--
-- Name: az_test_step_run; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE az_test_step_run (
    test_step_run_id bigint NOT NULL,
    test_run_id bigint NOT NULL,
    test_id bigint NOT NULL,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    status character varying(20),
    seq_id bigint,
    param1 character varying(4000)
);

--
-- Name: az_test_step_run_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_step_run
    ADD CONSTRAINT az_test_step_run_pk PRIMARY KEY (test_step_run_id);

--
-- Name: tf_trg_az_test_step_run_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_az_test_step_run_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TEST_STEP_RUN_ID is null then
 select nextval('tm_cz.SEQ_CZ_TEST') into NEW.TEST_STEP_RUN_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_az_test_step_run_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_az_test_step_run_id BEFORE INSERT ON az_test_step_run FOR EACH ROW EXECUTE PROCEDURE tf_trg_az_test_step_run_id();

--
-- Name: az_test_step_run_cz_job_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_step_run
    ADD CONSTRAINT az_test_step_run_cz_job_fk1 FOREIGN KEY (test_id) REFERENCES cz_test(test_id);

--
-- Name: az_tst_step_run_az_test_ru_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_step_run
    ADD CONSTRAINT az_tst_step_run_az_test_ru_fk1 FOREIGN KEY (test_run_id) REFERENCES az_test_run(test_run_id);

