--
-- Name: bio_data_omic_marker; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_omic_marker (
    bio_data_id bigint,
    bio_marker_id bigint NOT NULL,
    data_table character varying(5)
);

--
-- Name: bio_d_o_m_marker2_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_d_o_m_marker2_pk ON bio_data_omic_marker USING btree (bio_marker_id, bio_data_id);

--
-- Name: bio_data_o_m_did_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_data_o_m_did_idx ON bio_data_omic_marker USING btree (bio_data_id);

