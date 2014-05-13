--
-- Name: bio_marker_correl_mv; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_marker_correl_mv (
    bio_marker_id bigint,
    asso_bio_marker_id bigint,
    correl_type character varying(18),
    mv_id bigint
);
