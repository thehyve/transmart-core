--
-- Name: bio_lit_sum_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_sum_data (
    bio_lit_sum_data_id bigint NOT NULL,
    etl_id character varying(50),
    disease_site character varying(250),
    target character varying(50),
    variant character varying(50),
    data_type character varying(50),
    alteration_type character varying(100),
    total_frequency character varying(50),
    total_affected_cases character varying(50),
    summary character varying(1000)
);

--
-- Name: bio_lit_sum_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_sum_data
    ADD CONSTRAINT bio_lit_sum_data_pk PRIMARY KEY (bio_lit_sum_data_id);

--
-- Name: tf_trg_bio_lit_sum_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_sum_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_SUM_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_SUM_DATA_ID ;
       end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_bio_lit_sum_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_sum_data_id BEFORE INSERT ON bio_lit_sum_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_sum_data_id();

