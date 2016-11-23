--
-- Name: rwg_analysis; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE rwg_analysis (
    study_id character varying(500),
    cohorts character varying(500),
    analysis_id character varying(500),
    pvalue_cutoff double precision,
    foldchange_cutoff double precision,
    lsmean_cutoff double precision,
    analysis_type character varying(500),
    data_type character varying(500),
    platform character varying(500),
    long_desc character varying(500),
    short_desc character varying(500),
    import_date timestamp(6) without time zone DEFAULT now() NOT NULL,
    bio_assay_analysis_id bigint
);

