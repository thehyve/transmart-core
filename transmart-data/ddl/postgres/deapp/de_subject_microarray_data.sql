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
-- Name: COLUMN de_subject_microarray_data.trial_name; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.trial_name IS 'Name of the trial. E.g., SHARED_HD_CONCEPTS_STUDY_C_PR. Not used.';

--
-- Name: COLUMN de_subject_microarray_data.assay_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.assay_id IS 'Id used to link highdim data to assays in the de_subject_sample_mapping table.';

--
-- Name: COLUMN de_subject_microarray_data.patient_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.patient_id IS 'The patient id linking to the patient_dimension.';

--
-- Name: COLUMN de_subject_microarray_data.raw_intensity; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.raw_intensity IS 'Raw projection.';

--
-- Name: COLUMN de_subject_microarray_data.log_intensity; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.log_intensity IS 'Log projection.';

--
-- Name: COLUMN de_subject_microarray_data.zscore; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_subject_microarray_data.zscore IS 'Zscore projection.';

--
-- Name: de_subject_microarray_data de_subject_microarray_data_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_microarray_data
    ADD CONSTRAINT de_subject_microarray_data_pk PRIMARY KEY (assay_id, probeset_id);

