--
-- Name: de_subject_sample_mapping; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_sample_mapping (
    patient_id bigint,
    site_id character varying(100),
    subject_id character varying(100),
    subject_type character varying(100),
    concept_code character varying(1000),
    assay_id bigint NOT NULL,
    patient_uid character varying(50),
    sample_type character varying(100),
    assay_uid character varying(100),
    trial_name character varying(30),
    timepoint character varying(100),
    timepoint_cd character varying(50),
    sample_type_cd character varying(50),
    tissue_type_cd character varying(50),
    platform character varying(50),
    platform_cd character varying(50),
    tissue_type character varying(100),
    data_uid character varying(100),
    gpl_id character varying(50),
    rbm_panel character varying(50),
    sample_id bigint,
    sample_cd character varying(200),
    category_cd character varying(1000),
    source_cd character varying(200),
    omic_source_study character varying(200),
    omic_patient_num bigint,
    omic_patient_id bigint,
    partition_id numeric
);

--
-- Name: de_subject_smpl_mpng_idx1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_smpl_mpng_idx1 ON de_subject_sample_mapping USING btree (timepoint, patient_id, trial_name);

--
-- Name: de_subject_smpl_mpng_idx2; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_subject_smpl_mpng_idx2 ON de_subject_sample_mapping USING btree (patient_id, timepoint_cd, platform_cd, assay_id, trial_name);

--
-- Name: idx_de_subj_smpl_trial_ccode; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_de_subj_smpl_trial_ccode ON de_subject_sample_mapping USING btree (trial_name, concept_code);

--
-- add documentation
--
COMMENT ON TABLE deapp.de_subject_sample_mapping IS 'Table links patients to Highdim samples';

COMMENT ON COLUMN de_subject_sample_mapping.patient_id IS 'The patient id linking the patient_dimension';
COMMENT ON COLUMN de_subject_sample_mapping.subject_id IS 'links to subject_id in de_subject_microarray_data';
COMMENT ON COLUMN de_subject_sample_mapping.concept_code IS 'The highdim concept code. E.g. CTHD:HD:EXPLUNG';
COMMENT ON COLUMN de_subject_sample_mapping.assay_id IS 'Id that links to assay_id in highdim tables like de_subject_microarray_data and de_rnaseq_transcript_data';
COMMENT ON COLUMN de_subject_sample_mapping.trial_name IS 'Name of the trial this sample is part of.';
COMMENT ON COLUMN de_subject_sample_mapping.gpl_id IS 'id of the GPL platform for this sample. links to de_gpl_info table';
COMMENT ON COLUMN de_subject_sample_mapping.sample_cd IS 'Code linking to the sample';