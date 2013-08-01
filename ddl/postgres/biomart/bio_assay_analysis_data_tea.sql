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
    tea_rank bigint
);

--
-- Name: bio_aa_data_t_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data_tea
    ADD CONSTRAINT bio_aa_data_t_pk PRIMARY KEY (bio_asy_analysis_data_id);

--
-- Name: baadt_f_idx13; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_f_idx13 ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_asy_analysis_data_id);

--
-- Name: baadt_idex12; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idex12 ON bio_assay_analysis_data_tea USING btree (feature_group_name, bio_asy_analysis_data_id);

--
-- Name: baadt_idx10; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idx10 ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_experiment_id);

--
-- Name: baadt_idx11; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idx11 ON bio_assay_analysis_data_tea USING btree (bio_experiment_id, bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: baadt_idx17; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idx17 ON bio_assay_analysis_data_tea USING btree (bio_assay_analysis_id, tea_rank);

--
-- Name: baadt_idx6; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idx6 ON bio_assay_analysis_data_tea USING btree (bio_experiment_id, bio_assay_analysis_id);

--
-- Name: baadt_idx7; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baadt_idx7 ON bio_assay_analysis_data_tea USING btree (bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: idx_baadt_fg_ad; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_baadt_fg_ad ON bio_assay_analysis_data_tea USING btree (bio_assay_feature_group_id, bio_assay_analysis_id);

--
-- Name: idx_baadt_idx10; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_baadt_idx10 ON bio_assay_analysis_data_tea USING btree (bio_experiment_type, bio_asy_analysis_data_id);

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

