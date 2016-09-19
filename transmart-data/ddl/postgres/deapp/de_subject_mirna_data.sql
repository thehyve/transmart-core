--
-- Name: de_subject_mirna_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_mirna_data (
    trial_source character varying(200),
    trial_name character varying(50),
    assay_id numeric(18,0) NOT NULL,
    patient_id numeric(18,0),
    raw_intensity numeric,
    log_intensity numeric,
    probeset_id numeric(38,0) NOT NULL,
    zscore numeric(18,9),
    partition_id numeric
);

--
-- Name: de_subject_mirna_data_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_mirna_data
    ADD CONSTRAINT de_subject_mirna_data_pkey PRIMARY KEY (assay_id, probeset_id);

