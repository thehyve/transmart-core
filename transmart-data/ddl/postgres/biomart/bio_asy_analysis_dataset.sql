--
-- Name: bio_asy_analysis_dataset; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_dataset (
    bio_assay_dataset_id bigint NOT NULL,
    bio_assay_analysis_id bigint NOT NULL
);

--
-- Name: bio_data_analysis_dataset_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_dataset
    ADD CONSTRAINT bio_data_analysis_dataset_pk PRIMARY KEY (bio_assay_dataset_id, bio_assay_analysis_id);

--
-- Name: bio_asy_analysis_dataset_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_asy_analysis_dataset_pk ON bio_asy_analysis_dataset USING btree (bio_assay_dataset_id, bio_assay_analysis_id);

--
-- Name: bio_data_anl_ds_anl_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_dataset
    ADD CONSTRAINT bio_data_anl_ds_anl_fk FOREIGN KEY (bio_assay_analysis_id) REFERENCES bio_assay_analysis(bio_assay_analysis_id);

--
-- Name: bio_data_anl_ds_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_dataset
    ADD CONSTRAINT bio_data_anl_ds_fk FOREIGN KEY (bio_assay_dataset_id) REFERENCES bio_assay_dataset(bio_assay_dataset_id);

