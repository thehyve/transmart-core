--
-- Name: search_bio_mkr_correl_fast_mv; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_bio_mkr_correl_fast_mv (
    domain_object_id bigint NOT NULL,
    asso_bio_marker_id bigint,
    correl_type character varying(19),
    value_metric bigint,
    mv_id bigint
);

