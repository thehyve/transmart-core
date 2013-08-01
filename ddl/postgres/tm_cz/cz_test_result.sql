--
-- Name: cz_test_result; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_test_result (
    test_id bigint NOT NULL,
    test_result_id bigint NOT NULL,
    test_result_text character varying(2000),
    test_result_nbr bigint,
    test_run_id bigint,
    external_location character varying(2000),
    run_date timestamp without time zone,
    study_id character varying(2000)
);

--
-- Name: tf_trg_cz_test_result_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_test_result_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
if NEW.TEST_RESULT_ID is null then          
select nextval('tm_cz.SEQ_CZ') into NEW.TEST_RESULT_ID ;       end if;       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_test_result_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_test_result_id BEFORE INSERT ON cz_test_result FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_test_result_id();

