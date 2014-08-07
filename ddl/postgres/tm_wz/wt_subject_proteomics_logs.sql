--
-- Name: wt_subject_proteomics_logs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_proteomics_logs (
    probeset_id character varying(500),
    intensity_value numeric,
    pvalue double precision,
    num_calls numeric,
    assay_id numeric(18,0),
    patient_id numeric(18,0),
    sample_id numeric(18,0),
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    log_intensity numeric
);

--
-- Name: wt_subject_proteomics_logs_i1; Type: INDEX; Schema: tm_wz; Owner: -
--
--CREATE INDEX wt_subject_proteomics_logs_i1 ON wt_subject_proteomics_logs USING btree (trial_name, probeset_id);

