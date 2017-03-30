--
-- Name: bio_lit_inh_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_inh_data (
    bio_lit_inh_data_id bigint NOT NULL,
    bio_lit_ref_data_id bigint,
    etl_id character varying(50),
    trial_type character varying(250),
    trial_phase character varying(250),
    trial_status character varying(250),
    trial_experimental_model character varying(250),
    trial_tissue character varying(250),
    trial_body_substance character varying(250),
    trial_description character varying(1000),
    trial_designs character varying(250),
    trial_cell_line character varying(250),
    trial_cell_type character varying(250),
    trial_patients_number character varying(100),
    trial_inclusion_criteria character varying(2000),
    inhibitor character varying(250),
    inhibitor_standard_name character varying(250),
    casid character varying(250),
    description character varying(1000),
    concentration character varying(250),
    time_exposure character varying(500),
    administration character varying(250),
    treatment character varying(2000),
    techniques character varying(1000),
    effect_molecular character varying(250),
    effect_percent character varying(250),
    effect_number character varying(50),
    effect_value character varying(250),
    effect_sd character varying(250),
    effect_unit character varying(250),
    effect_response_rate character varying(250),
    effect_downstream character varying(2000),
    effect_beneficial character varying(2000),
    effect_adverse character varying(2000),
    effect_description character varying(2000),
    effect_pharmacos character varying(2000),
    effect_potentials character varying(2000)
);

--
-- Name: bio_lit_inh_data bio_lit_inh_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_inh_data
    ADD CONSTRAINT bio_lit_inh_data_pk PRIMARY KEY (bio_lit_inh_data_id);

--
-- Name: tf_trg_bio_lit_inh_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_inh_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_INH_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_INH_DATA_ID ;
       end if; 
       RETURN NEW;
end;
$$;

--
-- Name: bio_lit_inh_data trg_bio_lit_inh_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_inh_data_id BEFORE INSERT ON bio_lit_inh_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_inh_data_id();

--
-- Name: bio_lit_inh_data bio_lit_inh_ref_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_inh_data
    ADD CONSTRAINT bio_lit_inh_ref_fk FOREIGN KEY (bio_lit_ref_data_id) REFERENCES bio_lit_ref_data(bio_lit_ref_data_id);

