--
-- Name: ctd2_inhib_details; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd2_inhib_details (
    ctd_inhib_seq bigint,
    ctd_study_id bigint,
    common_name_name character varying(500),
    standard_name_name character varying(500),
    experimental_detail_dose character varying(4000),
    exp_detail_exposure_period character varying(4000),
    exp_detail_treatment_name character varying(4000),
    exp_detail_admin_route character varying(4000),
    exp_detail_description character varying(4000),
    exp_detail_placebo character varying(250),
    comparator_name_name character varying(250),
    comp_treatment_name character varying(4000),
    comp_admin_route character varying(4000),
    comp_dose character varying(2000),
    comp_exposure_period character varying(2000)
);

--
-- Name: tf_trg_ctd2_inhib_details(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_ctd2_inhib_details() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN     
	IF NEW.CTD_INHIB_SEQ IS NULL THEN 
		select nextval('biomart.SEQ_CLINICAL_TRIAL_DESIGN_ID') INTO NEW.CTD_INHIB_SEQ ;  
	END IF;    
	RETURN NEW;
END;
$$;

--
-- Name: trg_ctd2_inhib_details; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_ctd2_inhib_details BEFORE INSERT ON ctd2_inhib_details FOR EACH ROW EXECUTE PROCEDURE tf_trg_ctd2_inhib_details();

