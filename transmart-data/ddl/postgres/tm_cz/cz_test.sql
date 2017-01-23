--
-- Name: cz_test; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_test (
    test_id bigint NOT NULL,
    test_name character varying(200),
    test_desc character varying(1000),
    test_schema character varying(255),
    test_table character varying(255),
    test_column character varying(255),
    test_type character varying(255),
    test_sql character varying(2000),
    test_param1 character varying(2000),
    test_param2 character varying(2000),
    test_param3 character varying(2000),
    test_min_value double precision,
    test_max_value double precision,
    test_category_id bigint,
    test_severity_cd character varying(20),
    table_type character varying(100)
);

--
-- Name: cz_test_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_test
    ADD CONSTRAINT cz_test_pk PRIMARY KEY (test_id);

--
-- Name: tf_trg_cz_test_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_test_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin      
if NEW.TEST_ID is null then          
select nextval('tm_cz.SEQ_CZ') into NEW.TEST_ID ;       end if;       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_test_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_test_id BEFORE INSERT ON cz_test FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_test_id();

--
-- Name: cz_test_cz_test_category_fk1; Type: FK CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_test
    ADD CONSTRAINT cz_test_cz_test_category_fk1 FOREIGN KEY (test_category_id) REFERENCES cz_test_category(test_category_id);

