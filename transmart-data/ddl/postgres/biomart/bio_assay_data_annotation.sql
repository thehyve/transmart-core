--
-- Name: bio_assay_data_annotation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_data_annotation (
    bio_assay_feature_group_id bigint,
    bio_marker_id bigint NOT NULL,
    data_table character(5)
);

--
-- Name: bio_a_o_an_idx2; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_o_an_idx2 ON bio_assay_data_annotation USING btree (bio_assay_feature_group_id, bio_marker_id);

--
-- Name: bio_a_o_fg_id_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_o_fg_id_idx ON bio_assay_data_annotation USING btree (bio_assay_feature_group_id);

--
-- Name: bio_d_fg_m_marker2_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_d_fg_m_marker2_pk ON bio_assay_data_annotation USING btree (bio_marker_id, bio_assay_feature_group_id);

