--
-- Name: gse_analysis; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE gse_analysis (
    name character varying(100),
    platform character varying(100),
    test character varying(1000),
    data_ct bigint,
    fc_mean bigint,
    fc_stddev bigint,
    bio_experiment_id bigint,
    bio_assay_platform_id bigint,
    bio_assay_analysis_id bigint,
    analysis1 character varying(300),
    analysis2 character varying(300)
);

