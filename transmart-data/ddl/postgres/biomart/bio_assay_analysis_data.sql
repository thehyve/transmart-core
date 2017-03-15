--
-- Name: bio_assay_analysis_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis_data (
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
    bio_assay_feature_group_id bigint,
    probeset_id bigint,
    lsmean1 double precision,
    lsmean2 double precision
);

--
-- Name: baad_fgn_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_fgn_idx ON bio_assay_analysis_data USING btree (feature_group_name);

--
-- Name: baad_idx1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx1 ON bio_assay_analysis_data USING btree (bio_assay_feature_group_id, bio_experiment_id);

--
-- Name: baad_idx11; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx11 ON bio_assay_analysis_data USING btree (bio_experiment_id, bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: baad_idx12; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx12 ON bio_assay_analysis_data USING btree (bio_experiment_id, bio_assay_analysis_id, bio_assay_feature_group_id);

--
-- Name: baad_idx14; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx14 ON bio_assay_analysis_data USING btree (bio_assay_feature_group_id, bio_asy_analysis_data_id);

--
-- Name: baad_idx4; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx4 ON bio_assay_analysis_data USING btree (bio_assay_platform_id);

--
-- Name: baad_idx6; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx6 ON bio_assay_analysis_data USING btree (bio_experiment_id, bio_assay_analysis_id);

--
-- Name: baad_idx7; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX baad_idx7 ON bio_assay_analysis_data USING btree (bio_assay_analysis_id, bio_asy_analysis_data_id);

--
-- Name: bad_idx13; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bad_idx13 ON bio_assay_analysis_data USING btree (bio_assay_analysis_id, bio_assay_feature_group_id);

--
-- Name: pk_baad; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX pk_baad ON bio_assay_analysis_data USING btree (bio_asy_analysis_data_id);

--
-- Name: tf_trg_bio_asy_analysis_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_asy_analysis_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF NEW.BIO_ASY_ANALYSIS_DATA_ID IS NULL
		THEN  select nextval('SEQ_BIO_DATA_ID') INTO NEW.BIO_ASY_ANALYSIS_DATA_ID;
	END IF;
	RETURN NEW;
END
$$;

--
-- Name: bio_assay_analysis_data trg_bio_asy_analysis_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_asy_analysis_data_id BEFORE INSERT ON bio_assay_analysis_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_asy_analysis_data_id();

--
-- Name: bio_assay_analysis_data bio_assay_analysis_data_n_fk1; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data
    ADD CONSTRAINT bio_assay_analysis_data_n_fk1 FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

--
-- Name: bio_assay_analysis_data bio_assay_analysis_data_n_fk2; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data
    ADD CONSTRAINT bio_assay_analysis_data_n_fk2 FOREIGN KEY (bio_experiment_id) REFERENCES bio_experiment(bio_experiment_id);

--
-- Name: bio_assay_analysis_data bio_assay_analysis_data_n_fk3; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data
    ADD CONSTRAINT bio_assay_analysis_data_n_fk3 FOREIGN KEY (bio_assay_platform_id) REFERENCES bio_assay_platform(bio_assay_platform_id);

--
-- Name: bio_assay_analysis_data bio_asy_ad_fg_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis_data
    ADD CONSTRAINT bio_asy_ad_fg_fk FOREIGN KEY (bio_assay_feature_group_id) REFERENCES bio_assay_feature_group(bio_assay_feature_group_id);

