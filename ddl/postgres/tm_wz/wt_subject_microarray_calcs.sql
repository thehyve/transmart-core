--
-- Name: wt_subject_microarray_calcs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_microarray_calcs (
    trial_name character varying(50),
    probeset_id bigint,
    mean_intensity numeric,
    median_intensity numeric,
    stddev_intensity numeric
);

--
-- Name: wt_subject_mrna_calcs_i1; Type: INDEX; Schema: tm_wz; Owner: -
--
CREATE INDEX wt_subject_mrna_calcs_i1 ON wt_subject_microarray_calcs USING btree (trial_name, probeset_id);

