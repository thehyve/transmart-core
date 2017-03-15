--
-- Name: bio_taxonomy; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_taxonomy (
    bio_taxonomy_id bigint NOT NULL,
    taxon_name character varying(200) NOT NULL,
    taxon_label character varying(200) NOT NULL,
    ncbi_tax_id character varying(200)
);

--
-- Name: bio_taxonomy bio_taxon_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_taxonomy
    ADD CONSTRAINT bio_taxon_pk PRIMARY KEY (bio_taxonomy_id);

--
-- Name: tf_trg_bio_taxon_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_taxon_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN IF NEW.BIO_TAXONOMY_ID IS NULL THEN
  select nextval('biomart.SEQ_BIO_DATA_ID') INTO NEW.BIO_TAXONOMY_ID ;
END IF;
RETURN NEW;
END;
$$;

--
-- Name: bio_taxonomy trg_bio_taxon_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_taxon_id BEFORE INSERT ON bio_taxonomy FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_taxon_id();

