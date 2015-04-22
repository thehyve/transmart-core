--
-- Name: de_subject_sample_mapping_concept_code_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_sample_mapping
    ADD CONSTRAINT de_subject_sample_mapping_concept_code_fk FOREIGN KEY (concept_code) REFERENCES i2b2demodata.concept_dimension(concept_cd) ON DELETE CASCADE;

--
-- Name: de_subject_sample_mapping_patient_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_sample_mapping
    ADD CONSTRAINT de_subject_sample_mapping_patient_id_fk FOREIGN KEY (patient_id) REFERENCES i2b2demodata.patient_dimension(patient_num) ON DELETE CASCADE;

