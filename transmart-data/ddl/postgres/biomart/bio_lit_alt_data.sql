--
-- Name: bio_lit_alt_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_alt_data (
    bio_lit_alt_data_id bigint NOT NULL,
    bio_lit_ref_data_id bigint NOT NULL,
    in_vivo_model_id bigint,
    in_vitro_model_id bigint,
    etl_id character varying(50),
    alteration_type character varying(50),
    control character varying(1000),
    effect character varying(500),
    description character varying(1000),
    techniques character varying(1000),
    patients_percent character varying(500),
    patients_number character varying(500),
    pop_number character varying(250),
    pop_inclusion_criteria character varying(1000),
    pop_exclusion_criteria character varying(1000),
    pop_description character varying(1000),
    pop_type character varying(250),
    pop_value character varying(250),
    pop_phase character varying(250),
    pop_status character varying(250),
    pop_experimental_model character varying(250),
    pop_tissue character varying(250),
    pop_body_substance character varying(250),
    pop_localization character varying(1000),
    pop_cell_type character varying(250),
    clin_submucosa_marker_type character varying(250),
    clin_submucosa_unit character varying(250),
    clin_submucosa_value character varying(250),
    clin_asm_marker_type character varying(250),
    clin_asm_unit character varying(250),
    clin_asm_value character varying(250),
    clin_cellular_source character varying(250),
    clin_cellular_type character varying(250),
    clin_cellular_count character varying(250),
    clin_prior_med_percent character varying(250),
    clin_prior_med_dose character varying(250),
    clin_prior_med_name character varying(250),
    clin_baseline_variable character varying(250),
    clin_baseline_percent character varying(250),
    clin_baseline_value character varying(250),
    clin_smoker character varying(250),
    clin_atopy character varying(250),
    control_exp_percent character varying(50),
    control_exp_number character varying(50),
    control_exp_value character varying(50),
    control_exp_sd character varying(50),
    control_exp_unit character varying(100),
    over_exp_percent character varying(50),
    over_exp_number character varying(50),
    over_exp_value character varying(50),
    over_exp_sd character varying(50),
    over_exp_unit character varying(100),
    loss_exp_percent character varying(50),
    loss_exp_number character varying(50),
    loss_exp_value character varying(50),
    loss_exp_sd character varying(50),
    loss_exp_unit character varying(100),
    total_exp_percent character varying(50),
    total_exp_number character varying(50),
    total_exp_value character varying(50),
    total_exp_sd character varying(50),
    total_exp_unit character varying(100),
    glc_control_percent character varying(250),
    glc_molecular_change character varying(250),
    glc_type character varying(50),
    glc_percent character varying(100),
    glc_number character varying(100),
    ptm_region character varying(250),
    ptm_type character varying(250),
    ptm_change character varying(250),
    loh_loci character varying(250),
    mutation_type character varying(250),
    mutation_change character varying(250),
    mutation_sites character varying(250),
    epigenetic_region character varying(250),
    epigenetic_type character varying(250)
);

--
-- Name: bio_lit_alt_data bio_lit_alt_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_alt_data
    ADD CONSTRAINT bio_lit_alt_data_pk PRIMARY KEY (bio_lit_alt_data_id);

--
-- Name: tf_trg_bio_lit_alt_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_alt_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_ALT_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_ALT_DATA_ID ;
       end if;
       RETURN NEW;
end;
$$;

--
-- Name: bio_lit_alt_data trg_bio_lit_alt_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_alt_data_id BEFORE INSERT ON bio_lit_alt_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_alt_data_id();

--
-- Name: bio_lit_alt_data bio_lit_alt_ref_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_alt_data
    ADD CONSTRAINT bio_lit_alt_ref_fk FOREIGN KEY (bio_lit_ref_data_id) REFERENCES bio_lit_ref_data(bio_lit_ref_data_id);

