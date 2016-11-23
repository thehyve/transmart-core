--
-- Name: wt_subject_rnaseq_calcs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE tm_wz.wt_subject_rnaseq_calcs
(
  --WL--trial_name character varying(200),
  region_id bigint,
  mean_readcount numeric,
  median_readcount numeric,
  stddev_readcount numeric
);

--
-- Name: wt_subject_rnaseq_calcs_i1; Type: INDEX; Schema: tm_wz; Owner: -
--
CREATE INDEX wt_subject_rnaseq_calcs_i1 ON wt_subject_rnaseq_calcs USING btree (region_id);

