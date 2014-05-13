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
-- Type: REF_CONSTRAINT; Owner: DEAPP; Name: SYS_C0010757
--
ALTER TABLE de_subject_metabolomics_data ADD FOREIGN KEY (metabolite_annotation_id)
 REFERENCES de_metabolite_annotation(id);
