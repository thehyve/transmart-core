--
-- Name: bio_lit_model_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_model_data (
    bio_lit_model_data_id bigint NOT NULL,
    etl_id character varying(50),
    model_type character varying(50),
    description character varying(1000),
    stimulation character varying(1000),
    control_challenge character varying(500),
    challenge character varying(1000),
    sentization character varying(1000),
    zygosity character varying(250),
    experimental_model character varying(250),
    animal_wild_type character varying(250),
    tissue character varying(250),
    cell_type character varying(250),
    cell_line character varying(250),
    body_substance character varying(250),
    component character varying(250),
    gene_id character varying(250)
);

--
-- Name: bio_lit_model_data bio_lit_model_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_model_data
    ADD CONSTRAINT bio_lit_model_data_pk PRIMARY KEY (bio_lit_model_data_id);

--
-- Name: tf_trg_bio_lit_model_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_model_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_MODEL_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_MODEL_DATA_ID ;
       end if;
       RETURN NEW;
end;
$$;

--
-- Name: bio_lit_model_data trg_bio_lit_model_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_model_data_id BEFORE INSERT ON bio_lit_model_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_model_data_id();

