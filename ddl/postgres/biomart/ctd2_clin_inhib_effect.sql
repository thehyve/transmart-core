--
-- Name: ctd2_clin_inhib_effect; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd2_clin_inhib_effect (
    ctd_cie_seq bigint,
    ctd_study_id bigint,
    event_description_name character varying(4000),
    event_definition_name character varying(4000),
    adverse_effect_name character varying(4000),
    signal_effect_name character varying(4000),
    pharmaco_parameter_name character varying(500),
    discontinuation_rate_value character varying(250),
    beneficial_effect_name character varying(4000),
    drug_effect character varying(4000),
    clinical_variable_name character varying(250),
    qp_sm_percentage_change character varying(250),
    qp_sm_absolute_change character varying(250),
    qp_sm_rate_of_change character varying(250),
    qp_sm_treatment_period character varying(250),
    qp_sm_group_change character varying(250),
    qp_sm_p_value character varying(250),
    ce_sm_no character varying(250),
    ce_sm_event_rate character varying(250),
    ce_time_to_event character varying(250),
    ce_reduction character varying(250),
    ce_p_value character varying(250),
    clinical_correlation character varying(2000),
    coefficient_value character varying(250),
    statistics_p_value character varying(250),
    statistics_description character varying(4000),
    primary_endpoint_type character varying(250),
    primary_endpoint_definition character varying(4000),
    primary_endpoint_test_name character varying(2000),
    primary_endpoint_time_period character varying(2000),
    primary_endpoint_change character varying(2000),
    primary_endpoint_p_value character varying(2000),
    secondary_endpoint_type character varying(2000),
    secondary_endpoint_definition character varying(4000),
    secondary_endpoint_test_name character varying(2000),
    secondary_endpoint_time_period character varying(4000),
    secondary_endpoint_change character varying(4000),
    secondary_endpoint_p_value character varying(4000)
);

--
-- Name: tf_trg_ctd2_clin_inhib_effect(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_ctd2_clin_inhib_effect() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN     
 	IF NEW.CTD_CIE_SEQ IS NULL THEN 
		select nextval('biomart.SEQ_CLINICAL_TRIAL_DESIGN_ID') INTO NEW.CTD_CIE_SEQ ;  
	END IF;  
	RETURN NEW;  
END;
$$;

--
-- Name: trg_ctd2_clin_inhib_effect; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_ctd2_clin_inhib_effect BEFORE INSERT ON ctd2_clin_inhib_effect FOR EACH ROW EXECUTE PROCEDURE tf_trg_ctd2_clin_inhib_effect();

--
-- Name: seq_clinical_trial_design_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_clinical_trial_design_id
    START WITH 24181
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

