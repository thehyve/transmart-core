--
-- Name: bio_assay_geno_platform_probe; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_geno_platform_probe (
    bio_asy_geno_platform_probe_id numeric(18,0) NOT NULL,
    bio_assay_platform_id bigint NOT NULL,
    orig_chrom character varying(5),
    orig_position numeric,
    orig_genome_build character varying(20),
    probe_name character varying(200),
    is_control boolean,
    created_by character varying(30),
    created_date date,
    modified_by character varying(30),
    modified_date date
);

--
-- Name: bio_assay_geno_platform_probe pk_bio_asy_gp_probe; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_geno_platform_probe
    ADD CONSTRAINT pk_bio_asy_gp_probe PRIMARY KEY (bio_asy_geno_platform_probe_id);

--
-- Name: idx_bio_assay_gp_probe_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX idx_bio_assay_gp_probe_pk ON bio_assay_geno_platform_probe USING btree (bio_asy_geno_platform_probe_id);

--
-- Name: idx_bio_asy_gpp_platform; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_asy_gpp_platform ON bio_assay_geno_platform_probe USING btree (bio_assay_platform_id);

--
-- Name: idx_bio_asy_gpp_probe_name; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_bio_asy_gpp_probe_name ON bio_assay_geno_platform_probe USING btree (probe_name);

--
-- Name: fun_geno_platform_probe_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION fun_geno_platform_probe_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
      IF NEW.BIO_ASY_GENO_PLATFORM_PROBE_ID IS NULL
      THEN
         SELECT NEXTVAL('BIOMART.SEQ_GENO_PLATFORM_PROBE_ID')
           INTO NEW.BIO_ASY_GENO_PLATFORM_PROBE_ID;
      END IF;
END;
$$;

--
-- Name: bio_assay_geno_platform_probe trg_geno_platform_probe_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_geno_platform_probe_id BEFORE INSERT ON bio_assay_geno_platform_probe FOR EACH ROW EXECUTE PROCEDURE fun_geno_platform_probe_id();

--
-- Name: bio_assay_geno_platform_probe fk_bio_asy_gp_probe_asy_plf; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_geno_platform_probe
    ADD CONSTRAINT fk_bio_asy_gp_probe_asy_plf FOREIGN KEY (bio_assay_platform_id) REFERENCES bio_assay_platform(bio_assay_platform_id);

--
-- Name: seq_geno_platform_probe_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_geno_platform_probe_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

