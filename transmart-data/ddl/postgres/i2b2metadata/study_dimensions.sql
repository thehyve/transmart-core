--
-- Name: study_dimensions; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE study_dimensions (
    dimension_description_id bigint NOT NULL,
    study_id bigint NOT NULL
);

--
-- Name: study_dimensions_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimensions
    ADD CONSTRAINT study_dimensions_pkey PRIMARY KEY (study_id, dimension_description_id);

--
-- Name: fk_9vvv5m5nbxsu2lkbeeftevd5j; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimensions
    ADD CONSTRAINT fk_9vvv5m5nbxsu2lkbeeftevd5j FOREIGN KEY (dimension_description_id) REFERENCES dimension_description(id);

