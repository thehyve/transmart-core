--
-- Type: SEQUENCE; Owner: TM_CZ; Name: SEQ_CZ_DATA
--
CREATE SEQUENCE seq_cz_data
  NO MINVALUE
  NO MAXVALUE
  INCREMENT BY 1
  START WITH 5
  CACHE 2
;

--
-- Name: cz_data; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_data (
    data_id bigint NOT NULL,
    data_name character varying(200),
    technical_desc character varying(1000),
    business_desc character varying(1000),
    create_date timestamp without time zone,
    custodian_id bigint,
    owner_id bigint,
    load_freq character varying(20)
);

--
-- Name: tf_trg_cz_data_id; Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.DATA_ID is null then
 select nextval('tm_cz.SEQ_CZ_DATA') into NEW.DATA_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_data_id(); Type: TRIGGER; Schema: tm_cz; Owner: -
--
  CREATE TRIGGER trg_cz_data_id BEFORE INSERT ON cz_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_data_id();


--
-- Name: cz_data_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_data
    ADD CONSTRAINT cz_data_pk PRIMARY KEY (data_id);
