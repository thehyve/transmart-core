--
-- Name: wt_subject_microarray_logs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_microarray_logs (
    probeset_id bigint,
    intensity_value numeric,
    pvalue double precision,
    num_calls bigint,
    assay_id bigint,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    log_intensity numeric,
    raw_intensity numeric
);

--
-- Name: wt_subject_mrna_logs_i1; Type: INDEX; Schema: tm_wz; Owner: -
--
CREATE INDEX wt_subject_mrna_logs_i1 ON wt_subject_microarray_logs USING btree (trial_name, probeset_id);

