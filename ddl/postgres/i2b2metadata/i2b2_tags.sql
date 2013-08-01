--
-- Name: i2b2_tags; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2_tags (
    tag_id integer NOT NULL,
    path character varying(400),
    tag character varying(400),
    tag_type character varying(400),
    tags_idx integer NOT NULL
);

