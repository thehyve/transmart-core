--
-- Name: bio_assay_sample; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_sample (
    bio_assay_id bigint NOT NULL,
    bio_sample_id bigint NOT NULL,
    bio_clinic_trial_timepoint_id bigint NOT NULL
);

--
-- Name: bio_assay_sample_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_sample
    ADD CONSTRAINT bio_assay_sample_pk PRIMARY KEY (bio_assay_id, bio_sample_id, bio_clinic_trial_timepoint_id);

--
-- Name: bio_assay_sample_bio_assay_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_sample
    ADD CONSTRAINT bio_assay_sample_bio_assay_fk FOREIGN KEY (bio_assay_id) REFERENCES bio_assay(bio_assay_id);

--
-- Name: bio_assay_sample_bio_sample_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_sample
    ADD CONSTRAINT bio_assay_sample_bio_sample_fk FOREIGN KEY (bio_sample_id) REFERENCES bio_sample(bio_sample_id);

