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


