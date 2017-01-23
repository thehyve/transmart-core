--
-- Name: bio_lit_pe_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_pe_data (
    bio_lit_pe_data_id bigint NOT NULL,
    bio_lit_ref_data_id bigint NOT NULL,
    in_vivo_model_id bigint,
    in_vitro_model_id bigint,
    etl_id character varying(50),
    description character varying(2000)
);

--
-- Name: bio_lit_pe_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_pe_data
    ADD CONSTRAINT bio_lit_pe_data_pk PRIMARY KEY (bio_lit_pe_data_id);

--
-- Name: tf_trg_bio_lit_pe_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_pe_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_PE_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_PE_DATA_ID ;
       end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_bio_lit_pe_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_pe_data_id BEFORE INSERT ON bio_lit_pe_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_pe_data_id();

--
-- Name: bio_lit_pe_ref_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_pe_data
    ADD CONSTRAINT bio_lit_pe_ref_fk FOREIGN KEY (bio_lit_ref_data_id) REFERENCES bio_lit_ref_data(bio_lit_ref_data_id);

