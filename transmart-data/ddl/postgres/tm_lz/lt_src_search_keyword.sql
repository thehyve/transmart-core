--
-- Name: lt_src_search_keyword; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_src_search_keyword (
    keyword character varying(300),
    display_category character varying(300),
    data_category character varying(200),
    source_cd character varying(200),
    uid_prefix character varying(20),
    unique_id character varying(300),
    parent_term character varying(300),
    bio_data_id character varying(300)
);
