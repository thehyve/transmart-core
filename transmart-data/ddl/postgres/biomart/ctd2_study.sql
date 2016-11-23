--
-- Name: ctd2_study; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd2_study (
    ctd_study_id bigint,
    ref_article_protocol_id character varying(1000),
    reference_id integer NOT NULL,
    pubmed_id character varying(250),
    pubmed_title character varying(2000),
    protocol_id character varying(1000),
    protocol_title character varying(2000)
);

--
-- Name: tf_trg_ctd2_study_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_ctd2_study_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN     
	IF NEW.CTD_STUDY_ID IS NULL THEN 
		select nextval('biomart.SEQ_CLINICAL_TRIAL_DESIGN_ID') INTO NEW.CTD_STUDY_ID ;  
	END IF;    
	RETURN NEW;
END;
$$;

--
-- Name: trg_ctd2_study_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_ctd2_study_id BEFORE INSERT ON ctd2_study FOR EACH ROW EXECUTE PROCEDURE tf_trg_ctd2_study_id();

