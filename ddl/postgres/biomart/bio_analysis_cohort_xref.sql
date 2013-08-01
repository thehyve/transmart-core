--
-- Name: bio_analysis_cohort_xref; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_analysis_cohort_xref (
    study_id character varying(255),
    cohort_id character varying(255),
    analysis_cd character varying(255),
    bio_assay_analysis_id bigint
);

--
-- Name: bacx_idx1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bacx_idx1 ON bio_analysis_cohort_xref USING btree (bio_assay_analysis_id);

