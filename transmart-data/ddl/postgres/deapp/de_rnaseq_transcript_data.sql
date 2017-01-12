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

--
-- add documentation
--
COMMENT ON TABLE deapp.de_rnaseq_transcript_data IS 'Table holds rnaseq transcript Highdim values';

COMMENT ON COLUMN de_rnaseq_transcript_data.transcript_id IS 'id linking to the transcript';
COMMENT ON COLUMN de_rnaseq_transcript_data.assay_id IS 'primary key. Id used to link highdim data to patients via the de_subject_sample_mapping tabble';
COMMENT ON COLUMN de_rnaseq_transcript_data.readcount IS 'base count';
COMMENT ON COLUMN de_rnaseq_transcript_data.normalized_readcount IS 'normalized projection';
COMMENT ON COLUMN de_rnaseq_transcript_data.log_normalized_readcount IS 'log projection';
COMMENT ON COLUMN de_rnaseq_transcript_data.zscore IS 'zscore projection';
