--
-- Name: bio_data_uid_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE bio_data_uid_release (
    bio_data_id bigint NOT NULL,
    unique_id character varying(200) NOT NULL,
    bio_data_type character varying(100) NOT NULL,
    release_study character varying(200) NOT NULL
);

