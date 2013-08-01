--
-- Name: tmp_analysis_data_tea_rank; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE tmp_analysis_data_tea_rank (
    analysis_data_id bigint NOT NULL,
    analysis_id bigint NOT NULL,
    rank1 bigint
);

--
-- Name: tmp_a_d_tea_r_index1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX tmp_a_d_tea_r_index1 ON tmp_analysis_data_tea_rank USING btree (analysis_data_id);

