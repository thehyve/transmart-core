--
-- Name: concept_counts; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE concept_counts (
    concept_path character varying(500),
    parent_concept_path character varying(500),
    patient_count bigint
);

--
-- Name: idx_count_concept_path; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_count_concept_path ON concept_counts USING btree (concept_path);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.concept_counts IS 'Stores number of patients for which there are observations over the given concept.';

COMMENT ON COLUMN concept_counts.concept_path IS 'The concept path to which patient count is bound.';
COMMENT ON COLUMN concept_counts.parent_concept_path IS 'The concept path of the parent.';
COMMENT ON COLUMN concept_counts.patient_count IS 'The number of patients for which there are observations for the concept.';