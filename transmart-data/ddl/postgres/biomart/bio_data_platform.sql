--
-- Name: bio_data_platform; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_platform (
    bio_data_id bigint,
    bio_assay_platform_id bigint,
    etl_source character varying(100)
);

--
-- Name: bio_data_platform_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_data_platform_pk ON bio_data_platform USING btree (bio_data_id, bio_assay_platform_id);

