--
-- Name: bio_asy_analysis_gwas_top50; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_gwas_top50 (
    bio_assay_analysis_id bigint,
    analysis character varying(500),
    chrom character varying(4),
    pos numeric(10,0),
    rsgene character varying(200),
    rsid character varying(50),
    pvalue double precision,
    logpvalue double precision,
    extdata character varying(4000),
    rnum bigint,
    intronexon character varying(10),
    regulome character varying(10),
    recombinationrate numeric(18,6),
    beta double precision,
    standard_error double precision,
    effect_allele character varying(30),
    other_allele character varying(30),
    strand character varying(1)
);

--
-- Name: idx1_bio_asy_gwas_t50; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx1_bio_asy_gwas_t50 ON bio_asy_analysis_gwas_top50 USING btree (bio_assay_analysis_id);

--
-- Name: idx2_bio_asy_gwas_top50; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx2_bio_asy_gwas_top50 ON bio_asy_analysis_gwas_top50 USING btree (analysis);

--
-- Name: bio_asy_analysis_gwas_top50 fk_gwas_top50_asy_anal; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_gwas_top50
    ADD CONSTRAINT fk_gwas_top50_asy_anal FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

