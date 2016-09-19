--
-- Name: bio_lit_int_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_int_data (
    bio_lit_int_data_id bigint NOT NULL,
    bio_lit_ref_data_id bigint NOT NULL,
    in_vivo_model_id bigint,
    in_vitro_model_id bigint,
    etl_id character varying(50),
    source_component character varying(100),
    source_gene_id character varying(50),
    target_component character varying(100),
    target_gene_id character varying(50),
    interaction_mode character varying(250),
    regulation character varying(1000),
    mechanism character varying(250),
    effect character varying(500),
    localization character varying(500),
    region character varying(250),
    techniques character varying(1000)
);

--
-- Name: bio_lit_int_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_int_data
    ADD CONSTRAINT bio_lit_int_data_pk PRIMARY KEY (bio_lit_int_data_id);

--
-- Name: tf_trg_bio_lit_int_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_int_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_INT_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_INT_DATA_ID ;
       end if; 
       RETURN NEW;
end;
$$;

--
-- Name: trg_bio_lit_int_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_int_data_id BEFORE INSERT ON bio_lit_int_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_int_data_id();

--
-- Name: bio_lit_int_ref_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_int_data
    ADD CONSTRAINT bio_lit_int_ref_fk FOREIGN KEY (bio_lit_ref_data_id) REFERENCES bio_lit_ref_data(bio_lit_ref_data_id);

