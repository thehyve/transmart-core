--
-- Name: bio_assay_analysis_gwas; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_gwas (
    bio_asy_analysis_gwas_id bigint,
    bio_assay_analysis_id bigint,
    rs_id character varying(50),
    p_value_char character varying(100),
    etl_id bigint,
    ext_data character varying(4000),
    p_value double precision,
    log_p_value double precision
);

--
-- Name: bio_asy_analysis_gwas_id; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_gwas
    ADD CONSTRAINT bio_asy_analysis_gwas_id PRIMARY KEY (bio_asy_analysis_gwas_id);


