--
-- Name: de_qpcr_mirna_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_qpcr_mirna_annotation (
    id_ref character varying(100),
    probe_id character varying(100),
    mirna_symbol character varying(100),
    mirna_id character varying(100),
    probeset_id numeric(38,0) NOT NULL,
    organism character varying(200),
    gpl_id character varying(20)
);

--
-- Name: de_qpcr_mirna_annotation_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_qpcr_mirna_annotation
    ADD CONSTRAINT de_qpcr_mirna_annotation_pk PRIMARY KEY (probeset_id);

--
-- Name: de_qpcr_mirna_annotation_gpl_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_qpcr_mirna_annotation
    ADD CONSTRAINT de_qpcr_mirna_annotation_gpl_id_fk FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform) ON DELETE CASCADE;

