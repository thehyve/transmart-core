--
-- Name: tmp_assay_analysis_metrics; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE tmp_assay_analysis_metrics (
    bio_assay_analysis_id bigint NOT NULL,
    data_ct bigint,
    fc_mean bigint,
    fc_stddev bigint
);

--
-- Name: tmp_assay_analysis_metrics_pk; Type: CONSTRAINT; Schema: tm_wz; Owner: -
--
ALTER TABLE ONLY tmp_assay_analysis_metrics
    ADD CONSTRAINT tmp_assay_analysis_metrics_pk PRIMARY KEY (bio_assay_analysis_id);

