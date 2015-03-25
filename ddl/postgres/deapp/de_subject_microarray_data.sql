--
-- Name: de_subject_microarray_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_microarray_data (
    trial_name character varying(50),
    probeset_id bigint NOT NULL,
    assay_id bigint NOT NULL,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(50),
    raw_intensity double precision,
    log_intensity double precision,
    zscore double precision,
    new_raw double precision,
    new_log double precision,
    new_zscore double precision,
    trial_source character varying(200),
    partition_id numeric
);

--
-- Name: de_subject_microarray_data_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_microarray_data
    ADD CONSTRAINT de_subject_microarray_data_pk PRIMARY KEY (assay_id, probeset_id);

--
-- Name: de_subject_microarray_data_assay_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_microarray_data
    ADD CONSTRAINT de_subject_microarray_data_assay_id_fk FOREIGN KEY (assay_id) REFERENCES de_subject_sample_mapping(assay_id) ON DELETE CASCADE;
