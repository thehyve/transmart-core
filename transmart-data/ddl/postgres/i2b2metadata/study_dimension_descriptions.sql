--
-- Name: study_dimension_descriptions; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE study_dimension_descriptions (
    dimension_description_id bigint NOT NULL,
    study_id bigint NOT NULL
);

--
-- Name: study_dimension_descriptions_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimension_descriptions
    ADD CONSTRAINT study_dimension_descriptions_pkey PRIMARY KEY (study_id, dimension_description_id);

--
-- Name: fk_9vvv5m5nbxsu2lkbeeftevd5j; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimension_descriptions
    ADD CONSTRAINT fk_9vvv5m5nbxsu2lkbeeftevd5j FOREIGN KEY (dimension_description_id) REFERENCES dimension_description(id);

