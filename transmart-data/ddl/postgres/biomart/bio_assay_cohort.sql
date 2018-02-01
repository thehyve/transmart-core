--
-- Name: bio_assay_cohort; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_cohort (
    study_id character varying(500),
    cohort_id character varying(500),
    disease character varying(500),
    sample_type character varying(500),
    treatment character varying(500),
    organism character varying(500),
    pathology character varying(500),
    cohort_title character varying(500),
    short_desc character varying(500),
    long_desc character varying(500),
    import_date timestamp(6) without time zone NOT NULL,
    bio_assay_cohort_id bigint NOT NULL
);

--
-- Name: bio_assay_cohort pk_bio_assay_cohort; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_cohort
    ADD CONSTRAINT pk_bio_assay_cohort PRIMARY KEY (bio_assay_cohort_id);

--
-- Name: tf_trg_bio_assay_cohort_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_assay_cohort_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.BIO_ASSAY_COHORT_ID IS NULL
		THEN
		SELECT nextval('BIOMART.SEQ_BIO_DATA_ID') INTO NEW.BIO_ASSAY_COHORT_ID;
	END IF;
RETURN NEW;
END;
$$;

--
-- Name: bio_assay_cohort trg_bio_assay_cohort_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_assay_cohort_id BEFORE INSERT ON bio_assay_cohort FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_assay_cohort_id();

