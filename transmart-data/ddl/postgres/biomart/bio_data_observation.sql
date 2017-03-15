--
-- Name: bio_data_observation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_observation (
    bio_data_id bigint,
    bio_observation_id bigint,
    etl_source character varying(100)
);

--
-- Name: bio_data_observation_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_data_observation_pk ON bio_data_observation USING btree (bio_data_id, bio_observation_id);

