--
-- Name: bio_assay_analysis_ext; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_ext (
    bio_assay_analysis_ext_id bigint NOT NULL,
    bio_assay_analysis_id bigint NOT NULL,
    vendor character varying(500),
    vendor_type character varying(500),
    genome_version character varying(500),
    tissue character varying(500),
    cell_type character varying(500),
    population character varying(500),
    research_unit character varying(500),
    sample_size character varying(500),
    model_name character varying(100),
    model_desc character varying(500),
    sensitive_flag integer,
    sensitive_desc character varying(500),
    effect_type character varying(100),
    effect_units character varying(100),
    effect_error1_type character varying(100),
    effect_error2_type character varying(100),
    effect_error_desc character varying(1000),
    trait character varying(255),
    requestor character varying(100),
    data_owner character varying(100),
    sd_trait_population numeric
);

--
-- Name: bio_assay_analysis_ext pk_bio_asy_analy_ext; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_ext
    ADD CONSTRAINT pk_bio_asy_analy_ext PRIMARY KEY (bio_assay_analysis_ext_id);

--
-- Name: bio_assay_analysis_ext fk_asy_analysis_ext_analy; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_ext
    ADD CONSTRAINT fk_asy_analysis_ext_analy FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

