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
    raw_intensity double precision NOT NULL,
    log_intensity double precision NOT NULL,
    zscore double precision NOT NULL,
    partition_id numeric
);

--
-- Name: de_sj_met_data_met_ann_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_metabolomics_data
    ADD CONSTRAINT de_sj_met_data_met_ann_id_fk FOREIGN KEY (metabolite_annotation_id) REFERENCES de_metabolite_annotation(id);

