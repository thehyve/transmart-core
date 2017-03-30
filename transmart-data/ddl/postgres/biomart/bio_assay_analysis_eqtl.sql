--
-- Name: bio_assay_analysis_eqtl; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_eqtl (
    bio_asy_analysis_eqtl_id bigint NOT NULL,
    bio_assay_analysis_id bigint,
    rs_id character varying(50),
    gene character varying(50),
    p_value_char character varying(100),
    cis_trans character varying(10),
    distance_from_gene character varying(10),
    etl_id bigint,
    ext_data character varying(4000),
    p_value double precision,
    log_p_value double precision
);

--
-- Name: bio_assay_analysis_eqtl bio_assay_analysis_eqtl_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_eqtl
    ADD CONSTRAINT bio_assay_analysis_eqtl_pk PRIMARY KEY (bio_asy_analysis_eqtl_id);

