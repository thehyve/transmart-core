--
-- Name: bio_asy_platform_probe_anno; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_platform_probe_anno (
    bio_asy_platform_probe_anno_id numeric(22,0) NOT NULL,
    bio_asy_geno_platform_probe_id numeric(22,0) NOT NULL,
    genotype_probe_annotation_id numeric(22,0) NOT NULL,
    bio_assay_platform_id bigint NOT NULL,
    genome_build character varying(10),
    created_by character varying(30),
    created_date date,
    modified_by character varying(30),
    modified_date date
);

--
-- Name: idx_bio_asy_ppanno_anno; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_asy_ppanno_anno ON bio_asy_platform_probe_anno USING btree (genotype_probe_annotation_id);

--
-- Name: idx_bio_asy_ppanno_platform; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_asy_ppanno_platform ON bio_asy_platform_probe_anno USING btree (bio_assay_platform_id);

--
-- Name: idx_bio_asy_ppanno_probe; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_asy_ppanno_probe ON bio_asy_platform_probe_anno USING btree (bio_asy_geno_platform_probe_id);

--
-- Name: pk_bio_asy_platform_probe_anno; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX pk_bio_asy_platform_probe_anno ON bio_asy_platform_probe_anno USING btree (bio_asy_platform_probe_anno_id);

--
-- Name: fun_bio_asy_ppa(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION fun_bio_asy_ppa() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
      IF NEW.BIO_ASY_PLATFORM_PROBE_ANNO_ID IS NULL
      THEN
         SELECT NEXTVAL('BIOMART.SEQ_BIO_ASY_PPA')
           INTO NEW.BIO_ASY_PLATFORM_PROBE_ANNO_ID;
      END IF;
END;
$$;

--
-- Name: bio_asy_platform_probe_anno trg_bio_asy_ppa; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_asy_ppa BEFORE INSERT ON bio_asy_platform_probe_anno FOR EACH ROW EXECUTE PROCEDURE fun_bio_asy_ppa();

--
-- Name: bio_asy_platform_probe_anno fk_bio_asy_ppa_gp_probe; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_platform_probe_anno
    ADD CONSTRAINT fk_bio_asy_ppa_gp_probe FOREIGN KEY (bio_asy_geno_platform_probe_id) REFERENCES bio_assay_geno_platform_probe(bio_asy_geno_platform_probe_id);

--
-- Name: bio_asy_platform_probe_anno fk_bio_asy_ppa_platform; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_platform_probe_anno
    ADD CONSTRAINT fk_bio_asy_ppa_platform FOREIGN KEY (bio_assay_platform_id) REFERENCES bio_assay_platform(bio_assay_platform_id);

--
-- Name: bio_asy_platform_probe_anno fk_bio_asy_ppa_probe_anno; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_platform_probe_anno
    ADD CONSTRAINT fk_bio_asy_ppa_probe_anno FOREIGN KEY (genotype_probe_annotation_id) REFERENCES genotype_probe_annotation(genotype_probe_annotation_id);

--
-- Name: seq_bio_asy_ppa; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_bio_asy_ppa
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

