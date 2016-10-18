--
-- Name: de_rnaseq_transcript_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rnaseq_transcript_annotation (
    id bigint NOT NULL,
    gpl_id character varying(50),
    chromosome character varying(2),
    start_bp bigint,
    end_bp bigint,
    transcript character varying(100)
);

--
-- Name: de_rnaseq_transcript_annotation_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_annotation
    ADD CONSTRAINT de_rnaseq_transcript_annotation_pkey PRIMARY KEY (id);

--
-- Name: de_rnaseq_transcript_annotation_gpl_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_transcript_annotation
    ADD CONSTRAINT de_rnaseq_transcript_annotation_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform);

