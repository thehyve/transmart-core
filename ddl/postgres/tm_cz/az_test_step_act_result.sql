--
-- Name: az_test_step_act_result; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE az_test_step_act_result (
    test_step_act_result_id bigint NOT NULL,
    test_step_run_id bigint NOT NULL,
    act_record_cnt double precision,
    return_code character varying(30),
    return_message character varying(4000),
    return_error_stack character varying(4000),
    return_error_back_trace character varying(4000)
);

--
-- Name: az_test_step_act_result_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_step_act_result
    ADD CONSTRAINT az_test_step_act_result_pk PRIMARY KEY (test_step_act_result_id);

--
-- Name: tf_trg_az_test_step_act_result(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_az_test_step_act_result() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TEST_STEP_ACT_RESULT_ID is null then
 select nextval('tm_cz.SEQ_CZ_TEST') into NEW.TEST_STEP_ACT_RESULT_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_az_test_step_act_result; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_az_test_step_act_result BEFORE INSERT ON az_test_step_act_result FOR EACH ROW EXECUTE PROCEDURE tf_trg_az_test_step_act_result();

--
-- Name: az_test_step_act_result_az_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_step_act_result
    ADD CONSTRAINT az_test_step_act_result_az_fk1 FOREIGN KEY (test_step_run_id) REFERENCES az_test_step_run(test_step_run_id);

