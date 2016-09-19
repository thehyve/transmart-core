--
-- Name: az_test_run; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE az_test_run (
    test_run_id bigint NOT NULL,
    dw_version_id bigint,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    status character varying(20),
    return_code character varying(30),
    return_message character varying(4000),
    test_run_name character varying(200),
    param1 character varying(4000),
    test_category_id bigint
);

--
-- Name: az_test_run_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_run
    ADD CONSTRAINT az_test_run_pk PRIMARY KEY (test_run_id);

--
-- Name: tf_trg_az_test_run_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_az_test_run_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TEST_RUN_ID is null then
 select nextval('tm_cz.SEQ_CZ_TEST') into NEW.TEST_RUN_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: tf_trg_az_test_run_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER tf_trg_az_test_run_id BEFORE INSERT ON az_test_run FOR EACH ROW EXECUTE PROCEDURE tf_trg_az_test_run_id();

--
-- Name: az_test_run_cz_dw_version_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_run
    ADD CONSTRAINT az_test_run_cz_dw_version_fk1 FOREIGN KEY (dw_version_id) REFERENCES cz_dw_version(dw_version_id);

--
-- Name: az_test_run_cz_test_categ_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY az_test_run
    ADD CONSTRAINT az_test_run_cz_test_categ_fk1 FOREIGN KEY (test_category_id) REFERENCES cz_test_category(test_category_id);

--
-- Name: seq_cz_test; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_cz_test
    START WITH 8259
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 2;

