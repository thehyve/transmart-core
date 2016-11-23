--
-- Name: rwg_analysis_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE rwg_analysis_data (
    study_id character varying(200),
    probeset character varying(200),
    fold_change double precision,
    pvalue double precision,
    raw_pvalue double precision,
    min_lsmean double precision,
    max_lsmean double precision,
    analysis_cd character varying(100),
    bio_assay_analysis_id bigint,
    adjusted_pvalue double precision
);

