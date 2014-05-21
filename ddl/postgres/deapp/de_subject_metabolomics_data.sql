--
-- Name: de_subject_metabolomics_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_metabolomics_data (
    trial_source character varying(200),
    trial_name character varying(200),
    metabolite_annotation_id bigint,
    assay_id bigint,
    subject_id character varying(100),
    patient_id bigint,
    raw_intensity bigint,
    log_intensity bigint,
    zscore bigint NOT NULL
);

--
-- Name: de_subject_metabolomics_data_metabolite_annotation_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_metabolomics_data
    ADD CONSTRAINT de_subject_metabolomics_data_metabolite_annotation_id_fkey FOREIGN KEY (metabolite_annotation_id) REFERENCES de_metabolite_annotation(id);

