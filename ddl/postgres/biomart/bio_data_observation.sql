--
-- Name: bio_data_observation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_observation (
    bio_data_id bigint NOT NULL,
    bio_observation_id bigint NOT NULL,
    etl_source character varying(100)
);

