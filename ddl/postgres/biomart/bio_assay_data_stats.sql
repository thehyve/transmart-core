--
-- Name: bio_assay_data_stats; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_data_stats (
    bio_assay_data_stats_id bigint NOT NULL,
    bio_sample_count bigint,
    quartile_1 double precision,
    quartile_2 double precision,
    quartile_3 double precision,
    max_value double precision,
    min_value double precision,
    bio_sample_id bigint,
    feature_group_name character varying(120),
    value_normalize_method character varying(50),
    bio_experiment_id bigint,
    mean_value double precision,
    std_dev_value double precision,
    bio_assay_dataset_id bigint,
    bio_assay_feature_group_id bigint NOT NULL
);

--
-- Name: bio_asy_dt_stats_s_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_data_stats
    ADD CONSTRAINT bio_asy_dt_stats_s_pk PRIMARY KEY (bio_assay_data_stats_id);

--
-- Name: bio_a_d_s_ds__s_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_d_s_ds__s_idx ON bio_assay_data_stats USING btree (bio_assay_dataset_id);

--
-- Name: bio_a_d_s_exp__s_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_d_s_exp__s_idx ON bio_assay_data_stats USING btree (bio_experiment_id);

--
-- Name: bio_a_d_s_f_g_s_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_d_s_f_g_s_idx ON bio_assay_data_stats USING btree (feature_group_name, bio_assay_data_stats_id);

--
-- Name: bio_a_d_s_fgi_s_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_a_d_s_fgi_s_idx ON bio_assay_data_stats USING btree (bio_assay_feature_group_id, bio_assay_data_stats_id);

--
-- Name: bio_asy_dt_fg_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_data_stats
    ADD CONSTRAINT bio_asy_dt_fg_fk FOREIGN KEY (bio_assay_feature_group_id) REFERENCES bio_assay_feature_group(bio_assay_feature_group_id);

--
-- Name: bio_asy_dt_stat_exp_s_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_data_stats
    ADD CONSTRAINT bio_asy_dt_stat_exp_s_fk FOREIGN KEY (bio_experiment_id) REFERENCES bio_experiment(bio_experiment_id);

--
-- Name: bio_asy_dt_stats_ds_s_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_data_stats
    ADD CONSTRAINT bio_asy_dt_stats_ds_s_fk FOREIGN KEY (bio_assay_dataset_id) REFERENCES bio_assay_dataset(bio_assay_dataset_id);

--
-- Name: bio_asy_dt_stats_smp_s_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_data_stats
    ADD CONSTRAINT bio_asy_dt_stats_smp_s_fk FOREIGN KEY (bio_sample_id) REFERENCES bio_sample(bio_sample_id);

