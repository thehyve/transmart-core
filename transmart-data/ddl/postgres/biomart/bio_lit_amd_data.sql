--
-- Name: bio_lit_amd_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_amd_data (
    bio_lit_amd_data_id bigint NOT NULL,
    bio_lit_alt_data_id bigint NOT NULL,
    etl_id character varying(50),
    molecule character varying(50),
    molecule_type character varying(50),
    total_exp_percent character varying(50),
    total_exp_number character varying(100),
    total_exp_value character varying(100),
    total_exp_sd character varying(50),
    total_exp_unit character varying(50),
    over_exp_percent character varying(50),
    over_exp_number character varying(100),
    over_exp_value character varying(100),
    over_exp_sd character varying(50),
    over_exp_unit character varying(50),
    co_exp_percent character varying(50),
    co_exp_number character varying(100),
    co_exp_value character varying(100),
    co_exp_sd character varying(50),
    co_exp_unit character varying(50),
    mutation_type character varying(50),
    mutation_sites character varying(50),
    mutation_change character varying(50),
    mutation_percent character varying(50),
    mutation_number character varying(100),
    target_exp_percent character varying(50),
    target_exp_number character varying(100),
    target_exp_value character varying(100),
    target_exp_sd character varying(50),
    target_exp_unit character varying(50),
    target_over_exp_percent character varying(50),
    target_over_exp_number character varying(100),
    target_over_exp_value character varying(100),
    target_over_exp_sd character varying(50),
    target_over_exp_unit character varying(50),
    techniques character varying(250),
    description character varying(1000)
);

--
-- Name: bio_lit_amd_data bio_lit_amd_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_amd_data
    ADD CONSTRAINT bio_lit_amd_data_pk PRIMARY KEY (bio_lit_amd_data_id);

--
-- Name: tf_trg_bio_lit_amd_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_amd_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_AMD_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_AMD_DATA_ID ;
       end if;
       RETURN NEW;
    end;
$$;

--
-- Name: bio_lit_amd_data trg_bio_lit_amd_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_amd_data_id BEFORE INSERT ON bio_lit_amd_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_amd_data_id();

--
-- Name: bio_lit_amd_data bio_lit_amd_alt_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_amd_data
    ADD CONSTRAINT bio_lit_amd_alt_fk FOREIGN KEY (bio_lit_alt_data_id) REFERENCES bio_lit_alt_data(bio_lit_alt_data_id);

