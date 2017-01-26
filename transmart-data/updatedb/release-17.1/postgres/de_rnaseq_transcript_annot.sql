CREATE TABLE deapp.de_rnaseq_transcript_annot (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    ref_id character varying(50) NOT NULL,
    chromosome character varying(2),
    start_bp bigint,
    end_bp bigint,
    transcript character varying(100)
);

ALTER TABLE ONLY deapp.de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY deapp.de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_ref_id_unq UNIQUE (gpl_id, ref_id);

ALTER TABLE ONLY deapp.de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES deapp.de_gpl_info(platform);

GRANT SELECT ON TABLE deapp.de_rnaseq_transcript_annot TO biomart_user;
GRANT ALL ON TABLE deapp.de_rnaseq_transcript_annot TO deapp;
GRANT ALL ON TABLE deapp.de_rnaseq_transcript_annot TO tm_cz;