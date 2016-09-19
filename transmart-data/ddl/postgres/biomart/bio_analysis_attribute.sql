--
-- Name: bio_analysis_attribute; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_analysis_attribute (
    study_id character varying(255),
    bio_assay_analysis_id bigint NOT NULL,
    term_id bigint,
    source_cd character varying(255),
    bio_analysis_attribute_id bigint NOT NULL
);

--
-- Name: pk_baa_id; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_analysis_attribute
    ADD CONSTRAINT pk_baa_id PRIMARY KEY (bio_analysis_attribute_id);

--
-- Name: tf_trg_bio_analysis_attribute_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_analysis_attribute_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
	BEGIN
		if NEW.BIO_ANALYSIS_ATTRIBUTE_ID IS NULL
			THEN
			SELECT nextval('BIOMART.SEQ_BIO_DATA_ID') INTO NEW.BIO_ANALYSIS_ATTRIBUTE_ID;
  		END IF;
	RETURN NEW;
END;
$$;

--
-- Name: trg_bio_analysis_attribute_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_analysis_attribute_id BEFORE INSERT ON bio_analysis_attribute FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_analysis_attribute_id();

