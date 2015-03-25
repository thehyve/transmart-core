--
-- Name: de_subject_rnaseq_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_rnaseq_data (
    trial_name character varying(50),
    region_id bigint NOT NULL,
    assay_id bigint NOT NULL,
    patient_id bigint,
    readcount bigint,
    normalized_readcount double precision,
    log_normalized_readcount double precision,
    zscore double precision,
    partition_id bigint
);

--
-- Name: de_subject_rnaseq_data_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rnaseq_data
    ADD CONSTRAINT de_subject_rnaseq_data_pkey PRIMARY KEY (assay_id, region_id);

--
-- Name: de_subject_rnaseq_data_patient; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_rnaseq_data_patient ON de_subject_rnaseq_data USING btree (patient_id);

--
-- Name: de_subject_rnaseq_data_region; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_rnaseq_data_region ON de_subject_rnaseq_data USING btree (region_id);

--
-- Name: de_subj_rnaseq_region_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rnaseq_data
    ADD CONSTRAINT de_subj_rnaseq_region_id_fkey FOREIGN KEY (region_id) REFERENCES de_chromosomal_region(region_id);

--
-- Name: de_subject_rnaseq_data_assay_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rnaseq_data
    ADD CONSTRAINT de_subject_rnaseq_data_assay_id_fk FOREIGN KEY (assay_id) REFERENCES de_subject_sample_mapping(assay_id) ON DELETE CASCADE;

