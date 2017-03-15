--
-- Name: bio_assay_analysis_gwas; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_gwas (
    bio_asy_analysis_gwas_id bigint NOT NULL,
    bio_assay_analysis_id bigint,
    rs_id character varying(50),
    p_value_char character varying(100),
    p_value double precision,
    log_p_value double precision,
    etl_id bigint,
    ext_data character varying(4000),
    effect_allele character varying(30),
    other_allele character varying(30),
    pass_fail character varying(1),
    probe_name character varying(100),
    bio_asy_geno_platform_probe_id bigint,
    beta double precision,
    standard_error double precision,
    geno_platform_probe_id numeric(22,0),
    beta_old numeric,
    standard_error_old numeric,
    effect_allele_clob text,
    other_allele_clob text,
    created_by character varying(30),
    created_date date,
    modified_by character varying(30),
    modified_date date
);

--
-- Name: bio_assay_analysis_gwas bio_asy_analysis_gwas_id; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_gwas
    ADD CONSTRAINT bio_asy_analysis_gwas_id PRIMARY KEY (bio_asy_analysis_gwas_id);

--
-- Name: idx2_bio_assay_analysis_gwas; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx2_bio_assay_analysis_gwas ON bio_assay_analysis_gwas USING btree (rs_id);

--
-- Name: idx3_bio_assay_analysis_gwas; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX idx3_bio_assay_analysis_gwas ON bio_assay_analysis_gwas USING btree (bio_assay_analysis_id, rs_id);

--
-- Name: idx_gwas_geno_pltfm_probe_id; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_gwas_geno_pltfm_probe_id ON bio_assay_analysis_gwas USING btree (geno_platform_probe_id);

--
-- Name: bio_assay_analysis_gwas fk_gwas_bio_aa; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_gwas
    ADD CONSTRAINT fk_gwas_bio_aa FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

--
-- Name: bio_assay_analysis_gwas fk_gwas_geno_platform_probe; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_gwas
    ADD CONSTRAINT fk_gwas_geno_platform_probe FOREIGN KEY (geno_platform_probe_id) REFERENCES bio_assay_geno_platform_probe(bio_asy_geno_platform_probe_id);

