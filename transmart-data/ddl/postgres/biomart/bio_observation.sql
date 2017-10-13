--
-- Name: bio_observation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_observation (
    bio_observation_id bigint NOT NULL,
    obs_name character varying(200),
    obs_code character varying(50),
    obs_descr character varying(1000),
    etl_id character varying(50),
    obs_type character varying(20),
    obs_code_source character varying(20)
);

--
-- Name: bio_observation observationdim_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_observation
    ADD CONSTRAINT observationdim_pk PRIMARY KEY (bio_observation_id);

--
-- Name: tf_trg_bio_observation_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_observation_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_OBSERVATION_ID is null then
        select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_OBSERVATION_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_observation trg_bio_observation_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_observation_id BEFORE INSERT ON bio_observation FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_observation_id();

