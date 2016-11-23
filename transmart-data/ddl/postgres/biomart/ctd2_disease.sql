--
-- Name: ctd2_disease; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd2_disease (
    ctd_disease_seq bigint,
    ctd_study_id bigint,
    disease_type_name character varying(500),
    disease_common_name character varying(500),
    icd10_name character varying(250),
    mesh_name character varying(250),
    study_type_name character varying(2000),
    physiology_name character varying(500)
);

--
-- Name: tf_trg_ctd2_disease(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_ctd2_disease() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN     
	IF NEW.CTD_DISEASE_SEQ IS NULL THEN 
		select nextval('biomart.SEQ_CLINICAL_TRIAL_DESIGN_ID') INTO NEW.CTD_DISEASE_SEQ ;  
	END IF;    
	RETURN NEW;
END;
$$;

--
-- Name: trg_ctd2_disease; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_ctd2_disease BEFORE INSERT ON ctd2_disease FOR EACH ROW EXECUTE PROCEDURE tf_trg_ctd2_disease();

