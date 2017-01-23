--
-- Name: rwg_analysis_ext; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE rwg_analysis_ext (
    study_id character varying(500),
    cohorts character varying(500),
    analysis_id character varying(500),
    pvalue_cutoff character varying(500),
    foldchange_cutoff character varying(500),
    lsmean_cutoff character varying(500),
    analysis_type character varying(500),
    data_type character varying(500),
    platform character varying(500),
    long_desc character varying(500),
    short_desc character varying(500)
);

