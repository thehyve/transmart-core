--
-- Name: concept_counts; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE concept_counts (
    concept_path character varying(500) NOT NULL,
    parent_concept_path character varying(500),
    patient_count bigint
);

--
-- Name: concept_counts_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY concept_counts
    ADD CONSTRAINT concept_counts_pk PRIMARY KEY (concept_path);

--
-- Name: idx_count_concept_path; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_count_concept_path ON concept_counts USING btree (concept_path);

--
-- Name: concept_counts_concept_path_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY concept_counts
    ADD CONSTRAINT concept_counts_concept_path_fk FOREIGN KEY (concept_path) REFERENCES concept_dimension(concept_path) ON DELETE CASCADE;

