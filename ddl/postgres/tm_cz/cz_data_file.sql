--
-- Name: cz_data_file; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_data_file (
    file_name character varying(200),
    provider character varying(200),
    extraction_date timestamp without time zone,
    location character varying(500),
    data_id bigint NOT NULL,
    contact_id bigint,
    exp_record_cnt bigint,
    act_record_cnt bigint,
    url character varying(500),
    description character varying(2000),
    data_file_id bigint NOT NULL
);

--
-- Type: SEQUENCE; Owner: TM_CZ; Name: SEQ_CZ_DATA_FILE
--
CREATE SEQUENCE seq_cz_data_file
  NO MINVALUE
  NO MAXVALUE
  INCREMENT BY 1
  START WITH 6
  CACHE 2
;

--
-- Name: tf_trg_cz_data_file_id; Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_data_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.DATA_FILE_ID is null then
 select nextval('tm_cz.SEQ_CZ_DATA_FILE') into NEW.DATA_FILE_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_data_file_id(); Type: TRIGGER; Schema: tm_cz; Owner: -
--
  CREATE TRIGGER trg_cz_data_file_id BEFORE INSERT ON cz_data_file FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_data_file_id();

--
-- Name: CZ_DATA_FILE_CZ_DATA_FK1 Type: REF_CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE cz_data_file
  ADD CONSTRAINT cz_data_file_cz_data_fk1 FOREIGN KEY (data_id) REFERENCES cz_data(data_id);


--
-- Name: cz_data_file_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_data_file
    ADD CONSTRAINT cz_data_file_pk PRIMARY KEY (data_file_id);
