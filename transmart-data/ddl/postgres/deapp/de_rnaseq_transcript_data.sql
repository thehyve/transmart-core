--
-- Name: de_rnaseq_transcript_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rnaseq_transcript_data (
    transcript_id bigint NOT NULL,
    assay_id bigint NOT NULL,
    readcount bigint,
    normalized_readcount double precision,
    log_normalized_readcount double precision,
    zscore double precision
);

--
-- Name: de_rnaseq_transcript_data_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_pkey PRIMARY KEY (assay_id, transcript_id);

--
-- Name: de_rnaseq_transcript_data_transcript_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_transcript_id_fkey FOREIGN KEY (transcript_id) REFERENCES de_rnaseq_transcript_annot(id);