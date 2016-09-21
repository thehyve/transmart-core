--
-- Name: bio_stats_exp_marker; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_stats_exp_marker (
    bio_marker_id bigint NOT NULL,
    bio_experiment_id bigint NOT NULL,
    bio_stats_exp_marker_id bigint
);

--
-- Name: bio_s_e_m_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_stats_exp_marker
    ADD CONSTRAINT bio_s_e_m_pk PRIMARY KEY (bio_marker_id, bio_experiment_id);

--
-- Name: bio_stats_exp_mk_exp_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_stats_exp_mk_exp_idx ON bio_stats_exp_marker USING btree (bio_experiment_id);

--
-- Name: bio_stats_exp_mk_mk_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_stats_exp_mk_mk_idx ON bio_stats_exp_marker USING btree (bio_marker_id);

