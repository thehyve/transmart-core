--
-- Name: mirna_bio_marker; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mirna_bio_marker (
    bio_marker_id bigint NOT NULL,
    bio_marker_name character varying(200),
    bio_marker_description character varying(1000),
    organism character varying(200),
    primary_source_code character varying(200),
    primary_external_id character varying(200),
    bio_marker_type character varying(200) NOT NULL,
    UNIQUE (organism, primary_external_id)
);
