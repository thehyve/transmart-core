--
-- Name: i2b2_tags_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE i2b2_tags_release (
    tag_id bigint NOT NULL,
    path character varying(200),
    tag character varying(200),
    tag_type character varying(200),
    release_study character varying(200)
);

