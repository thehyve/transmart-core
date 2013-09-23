--
-- Name: de_subject_rnaseq_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_rnaseq_data (
    trial_name varchar(50),
    region_id  bigint,
    assay_id   bigint,
    patient_id bigint,
    readcount  bigint,
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
-- Name: de_subject_rnaseq_data_region_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rnaseq_data
    ADD CONSTRAINT de_subject_rnaseq_data_region_id_fkey FOREIGN KEY (region_id) REFERENCES de_chromosomal_region(region_id);

