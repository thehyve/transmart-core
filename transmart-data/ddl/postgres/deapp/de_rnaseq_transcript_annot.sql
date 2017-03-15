--
-- Name: de_rnaseq_transcript_annot; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rnaseq_transcript_annot (
    id bigint NOT NULL,
    gpl_id character varying(50) NOT NULL,
    ref_id character varying(50) NOT NULL,
    chromosome character varying(2),
    start_bp bigint,
    end_bp bigint,
    transcript character varying(100)
);

--
-- Name: de_rnaseq_transcript_annot de_rnaseq_transcript_annot_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_pkey PRIMARY KEY (id);

--
-- Name: de_rnaseq_transcript_annot de_rnaseq_transcript_annot_ref_id_unq; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_ref_id_unq UNIQUE (gpl_id, ref_id);

--
-- Name: de_rnaseq_transcript_annot de_rnaseq_transcript_annot_gpl_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_annot
    ADD CONSTRAINT de_rnaseq_transcript_annot_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform);

