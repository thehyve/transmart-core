--
-- Name: search_secure_object_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE search_secure_object_release (
    search_secure_object_id bigint,
    bio_data_id bigint,
    display_name character varying(100),
    data_type character varying(200),
    bio_data_unique_id character varying(200),
    release_study character varying(100)
);

