--
-- Name: bio_analysis_attribute_lineage; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_analysis_attribute_lineage (
    bio_analysis_att_lineage_id bigint NOT NULL,
    bio_analysis_attribute_id bigint NOT NULL,
    ancestor_term_id bigint NOT NULL,
    ancestor_search_keyword_id bigint NOT NULL
);

--
-- Name: pk_baal_id; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_analysis_attribute_lineage
    ADD CONSTRAINT pk_baal_id PRIMARY KEY (bio_analysis_att_lineage_id);

--
-- Name: tf_trg_bio_analysis_al_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_analysis_al_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
	IF NEW.BIO_ANALYSIS_ATT_LINEAGE_ID IS NULL
		THEN
		SELECT nextval('BIOMART.SEQ_BIO_DATA_ID') INTO NEW.BIO_ANALYSIS_ATT_LINEAGE_ID;
	END IF;
RETURN NEW;
END;
$$;

--
-- Name: trg_bio_analysis_al_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_analysis_al_id BEFORE INSERT ON bio_analysis_attribute_lineage FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_analysis_al_id();

