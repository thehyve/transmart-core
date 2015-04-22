--
-- Name: de_rnaseq_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rnaseq_annotation (
    gpl_id character varying(50),
    transcript_id character varying(50) NOT NULL,
    gene_symbol character varying(50),
    gene_id character varying(50),
    organism character varying(30),
    probeset_id bigint
);

--
-- Name: de_rnaseq_annotation_uq; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_annotation
    ADD CONSTRAINT de_rnaseq_annotation_uq UNIQUE (transcript_id, gene_id);

--
-- Name: de_rnaseq_annotation_gpl_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_rnaseq_annotation
    ADD CONSTRAINT de_rnaseq_annotation_gpl_id_fk FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform) ON DELETE CASCADE;

