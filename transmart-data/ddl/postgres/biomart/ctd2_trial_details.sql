--
-- Name: ctd2_trial_details; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd2_trial_details (
    ctd_td_seq bigint,
    ctd_study_id bigint,
    control character varying(2000),
    blinding_procedure character varying(2000),
    no_of_arms character varying(2000),
    sponsor character varying(2000),
    patient_studied character varying(2000),
    source_type character varying(2000),
    trial_description character varying(4000),
    arm_name character varying(250),
    patient_study character varying(250),
    class_type character varying(250),
    class_value character varying(250),
    asthma_duration character varying(250),
    region character varying(250),
    age character varying(100),
    gender character varying(100),
    minor_participation character varying(100),
    symptom_score character varying(100),
    rescue_medication character varying(2000),
    therapeutic_intervention character varying(255),
    smokers character varying(255),
    former_smokers character varying(255),
    never_smokers character varying(255),
    smoking_pack_years character varying(255),
    pulm_path_name character varying(255),
    pulm_path_pct character varying(50),
    pulm_path_value character varying(50),
    pulm_path_method character varying(255),
    allow_med_therapy_ocs character varying(1000),
    allow_med_therapy_ics character varying(1000),
    allow_med_therapy_laba character varying(1000),
    allow_med_therapy_ltra character varying(1000),
    allow_med_therapy_desc character varying(4000),
    allow_med_therapy_cortster character varying(1000),
    allow_med_therapy_immuno character varying(1000),
    allow_med_therapy_cyto character varying(1000),
    allow_med_treat_ocs character varying(1000),
    allow_med_treat_ics character varying(1000),
    allow_med_treat_laba character varying(1000),
    allow_med_treat_ltra character varying(1000),
    allow_med_treat_desc character varying(4000),
    allow_med_treat_cortster character varying(1000),
    allow_med_treat_immuno character varying(1000),
    allow_med_treat_cyto character varying(1000),
    pat_char_base_clin_var character varying(500),
    pat_char_base_clin_var_pct character varying(250),
    pat_char_base_clin_var_value character varying(250),
    biomarker_name_name character varying(250),
    pat_char_biomarker_pct character varying(250),
    pat_char_biomarker_value character varying(250),
    pat_char_cellinfo_name character varying(250),
    pat_char_cellinfo_type character varying(250),
    pat_char_cellinfo_count character varying(250),
    pat_char_priormed_name character varying(250),
    pat_char_priormed_pct character varying(500),
    pat_char_priormed_dose character varying(250),
    disease_phenotype_name character varying(1000),
    disease_severity_name character varying(500),
    incl_age character varying(2000),
    incl_difficult_to_treat character varying(2000),
    incl_disease_diagnosis character varying(2000),
    incl_steroid_dose character varying(2000),
    incl_laba character varying(2000),
    incl_ocs character varying(2000),
    incl_xolair character varying(2000),
    incl_ltra_inhibitor character varying(2000),
    incl_fev1 character varying(2000),
    incl_fev1_reversibility character varying(2000),
    incl_smoking character varying(2000),
    incl_tlc character varying(2000),
    incl_fvc character varying(2000),
    incl_dlco character varying(2000),
    incl_sgrq character varying(2000),
    incl_hrct character varying(2000),
    incl_biopsy character varying(2000),
    incl_dypsnea_on_exertion character varying(2000),
    incl_concomitant_med character varying(2000),
    incl_former_smokers character varying(2000),
    incl_never_smokers character varying(2000),
    incl_smoking_pack_years character varying(2000),
    incl_fev_fvc character varying(2000),
    trial_des_minimal_symptom character varying(2000),
    trial_des_rescue_med character varying(4000),
    trial_des_exclusion_criteria character varying(4000),
    trial_des_open_label_status character varying(250),
    trial_des_random_status character varying(250),
    trial_des_nature_of_trial character varying(250),
    trial_des_blinded_status character varying(250),
    trial_des_run_in_period character varying(2000),
    trial_des_treatment character varying(2000),
    trial_des_washout_period character varying(2000),
    trial_status_name character varying(2000),
    trial_phase_name character varying(2000)
);

--
-- Name: tf_trg_ctd2_trial_details(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_ctd2_trial_details() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN     
	IF NEW.CTD_TD_SEQ IS NULL THEN 
		select nextval('biomart.SEQ_CLINICAL_TRIAL_DESIGN_ID') INTO NEW.CTD_TD_SEQ ;  
	END IF;   
	RETURN NEW; 
END;
$$;

--
-- Name: ctd2_trial_details trg_ctd2_trial_details; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_ctd2_trial_details BEFORE INSERT ON ctd2_trial_details FOR EACH ROW EXECUTE PROCEDURE tf_trg_ctd2_trial_details();

