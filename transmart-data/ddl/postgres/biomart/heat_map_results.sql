--
-- Name: heat_map_results; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE heat_map_results (
    subject_id character varying(50),
    log_intensity double precision,
    cohort_id character varying(255),
    probe_id character varying(100),
    bio_assay_feature_group_id bigint,
    fold_change_ratio double precision,
    tea_normalized_pvalue double precision,
    bio_marker_name character varying(400),
    bio_marker_id bigint,
    search_keyword_id bigint,
    bio_assay_analysis_id bigint,
    trial_name character varying(50),
    significant smallint,
    gene_id character varying(200),
    assay_id bigint,
    preferred_pvalue double precision
);

