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
-- Name: COLUMN de_rnaseq_transcript_data.transcript_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.transcript_id IS 'Id of the transcript, linking to id in de_rnaseq_transcript_annot.';

--
-- Name: COLUMN de_rnaseq_transcript_data.assay_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.assay_id IS 'Primary key. Id used to link highdim data to assays in the de_subject_sample_mapping table.';

--
-- Name: COLUMN de_rnaseq_transcript_data.readcount; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.readcount IS 'Base count.';

--
-- Name: COLUMN de_rnaseq_transcript_data.normalized_readcount; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.normalized_readcount IS 'Normalized projection.';

--
-- Name: COLUMN de_rnaseq_transcript_data.log_normalized_readcount; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.log_normalized_readcount IS 'Log projection.';

--
-- Name: COLUMN de_rnaseq_transcript_data.zscore; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_rnaseq_transcript_data.zscore IS 'Zscore projection.';

--
-- Name: de_rnaseq_transcript_data de_rnaseq_transcript_data_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_pkey PRIMARY KEY (assay_id, transcript_id);

--
-- Name: de_rnaseq_transcript_data de_rnaseq_transcript_data_transcript_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_transcript_id_fkey FOREIGN KEY (transcript_id) REFERENCES de_rnaseq_transcript_annot(id);

