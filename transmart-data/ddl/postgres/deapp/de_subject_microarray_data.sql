--
-- Name: de_subject_microarray_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_microarray_data (
    trial_name character varying(50),
    probeset_id bigint,
    assay_id bigint,
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
-- Name: de_subject_microarray_data; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_microarray_data
    ADD CONSTRAINT de_subject_microarray_data_pk PRIMARY KEY (assay_id, probeset_id);

--
-- add documentation
--
COMMENT ON TABLE deapp.de_subject_microarray_data IS 'Table holds microarray data values.';

COMMENT ON COLUMN de_subject_microarray_data.trial_name IS 'Name of the trial. E.g., SHARED_HD_CONCEPTS_STUDY_C_PR. Not used.';
COMMENT ON COLUMN de_subject_microarray_data.assay_id IS 'Id used to link highdim data to assays in the de_subject_sample_mapping table.';
COMMENT ON COLUMN de_subject_microarray_data.patient_id IS 'The patient id linking to the patient_dimension.';
COMMENT ON COLUMN de_subject_microarray_data.raw_intensity IS 'Raw projection.';
COMMENT ON COLUMN de_subject_microarray_data.log_intensity IS 'Log projection.';
COMMENT ON COLUMN de_subject_microarray_data.zscore IS 'Zscore projection.';
