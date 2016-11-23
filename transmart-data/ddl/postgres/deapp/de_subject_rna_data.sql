--
-- Name: de_subject_rna_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_rna_data (
    trial_source character varying(200),
    trial_name character varying(50),
    probeset_id character varying(200),
    assay_id bigint,
    patient_id bigint,
    raw_intensity double precision,
    log_intensity double precision,
    zscore double precision,
    partition_id numeric
);

--
-- Name: idx_de_rna_data_1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_de_rna_data_1 ON de_subject_rna_data USING btree (trial_name, assay_id, probeset_id);

--
-- Name: idx_de_rna_data_2; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_de_rna_data_2 ON de_subject_rna_data USING btree (assay_id, probeset_id);

