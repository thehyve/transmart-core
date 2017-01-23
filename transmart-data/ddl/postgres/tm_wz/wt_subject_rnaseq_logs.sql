--
-- Name: wt_subject_rnaseq_logs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE tm_wz.wt_subject_rnaseq_logs
(
  region_id bigint,
  readcount bigint,
  assay_id bigint,
  patient_id bigint,
  trial_name character varying(200),
  log_readcount numeric,
  raw_readcount numeric
);

--
-- Name: wt_subject_rnaseq_logs_i1; Type: INDEX; Schema: tm_wz; Owner: -
--
CREATE INDEX wt_subject_rnaseq_logs_i1 ON wt_subject_rnaseq_logs USING btree (trial_name, region_id);

