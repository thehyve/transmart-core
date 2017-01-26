CREATE TABLE deapp.de_rnaseq_transcript_data (
    transcript_id bigint NOT NULL,
    assay_id bigint NOT NULL,
    readcount bigint,
    normalized_readcount double precision,
    log_normalized_readcount double precision,
    zscore double precision
);

ALTER TABLE ONLY deapp.de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_pkey PRIMARY KEY (assay_id, transcript_id);

ALTER TABLE ONLY deapp.de_rnaseq_transcript_data
    ADD CONSTRAINT de_rnaseq_transcript_data_transcript_id_fkey FOREIGN KEY (transcript_id) REFERENCES deapp.de_rnaseq_transcript_annot(id);

GRANT SELECT ON TABLE deapp.de_rnaseq_transcript_data TO biomart_user;
GRANT ALL ON TABLE deapp.de_rnaseq_transcript_data TO deapp;
GRANT ALL ON TABLE deapp.de_rnaseq_transcript_data TO tm_cz;