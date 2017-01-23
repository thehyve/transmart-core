--
-- Name: bio_assay_analysis_data_tea; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_data_tea (
    bio_asy_analysis_data_id bigint NOT NULL,
    fold_change_ratio bigint,
    raw_pvalue double precision,
    adjusted_pvalue double precision,
    r_value double precision,
    rho_value double precision,
    bio_assay_analysis_id bigint NOT NULL,
    adjusted_p_value_code character varying(100),
    feature_group_name character varying(100) NOT NULL,
    bio_experiment_id bigint,
    bio_assay_platform_id bigint,
    etl_id character varying(100),
    preferred_pvalue double precision,
    cut_value double precision,
    results_value character varying(100),
    numeric_value double precision,
    numeric_value_code character varying(50),
    tea_normalized_pvalue double precision,
    bio_experiment_type character varying(50),
    bio_assay_feature_group_id bigint,
    tea_rank bigint,
    probeset_id bigint
);

--
-- Name: bio_aa_data_t_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_aa_data_t_pk PRIMARY KEY (bio_asy_analysis_data_id);

--
-- Name: baad_idx_tea_analysis; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_analysis ON bio_assay_analysis_data_tea USING btree (bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: baad_idx_tea_exp_analysis; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_exp_analysis ON bio_assay_analysis_data_tea USING btree (bio_experiment_id, bio_assay_analysis_id);

--
-- Name: baad_idx_tea_exp_analysis1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_exp_analysis1 ON bio_assay_analysis_data_tea USING btree (bio_experiment_id, bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: baad_idx_tea_fg_experiment; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_fg_experiment ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_experiment_id);

--
-- Name: baad_idx_tea_probe_id; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_probe_id ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_asy_analysis_data_id);

--
-- Name: baad_idx_tea_probe_name; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_probe_name ON bio_assay_analysis_data_tea USING btree (feature_group_name, bio_asy_analysis_data_id);

--
-- Name: baad_idx_tea_rank; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx_tea_rank ON bio_assay_analysis_data_tea USING btree (bio_assay_analysis_id, tea_rank);

--
-- Name: idx_baad_idx_tea_experiment_type; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_baad_idx_tea_experiment_type ON bio_assay_analysis_data_tea USING btree (bio_experiment_type, bio_asy_analysis_data_id);

--
-- Name: idx_baad_idx_tea_probe_analysis; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_baad_idx_tea_probe_analysis ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_assay_analysis_id);

--
-- Name: bio_assay_analysis_data_t_fk1; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_assay_analysis_data_t_fk1 FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

--
-- Name: bio_assay_analysis_data_t_fk2; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_assay_analysis_data_t_fk2 FOREIGN KEY (bio_experiment_id) REFERENCES bio_experiment(bio_experiment_id);

--
-- Name: bio_assay_analysis_data_t_fk3; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_assay_analysis_data_t_fk3 FOREIGN KEY (bio_assay_platform_id) REFERENCES bio_assay_platform(bio_assay_platform_id);

--
-- Name: bio_asy_ad_tea_fg_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_asy_ad_tea_fg_fk FOREIGN KEY (bio_assay_feature_group_id) REFERENCES bio_assay_feature_group(bio_assay_feature_group_id);

