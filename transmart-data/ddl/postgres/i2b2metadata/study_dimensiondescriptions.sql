--
-- Name: study_dimensiondescriptions; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE study_dimensiondescriptions (
    dimension_description_id bigint NOT NULL,
    study_id bigint NOT NULL
);

--
-- Name: study_dimensiondescriptions_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimensiondescriptions
    ADD CONSTRAINT study_dimensiondescriptions_pkey PRIMARY KEY (study_id, dimension_description_id);

--
-- Name: fk_9vvv5m5nbxsu2lkbeeftevd5j; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study_dimensiondescriptions
    ADD CONSTRAINT fk_9vvv5m5nbxsu2lkbeeftevd5j FOREIGN KEY (dimension_description_id) REFERENCES dimension_description(id);

