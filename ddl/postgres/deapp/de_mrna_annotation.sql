--
-- Name: de_mrna_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_mrna_annotation (
    gpl_id character varying(100),
    probe_id character varying(100),
    gene_symbol character varying(100),
    probeset_id bigint NOT NULL,
    gene_id bigint,
    organism character varying(200)
);

--
-- Name: de_mrna_annotation_uq; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_mrna_annotation
    ADD CONSTRAINT de_mrna_annotation_uq UNIQUE (probeset_id, gene_id);

--
-- Name: de_mrna_annotation_idx1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_mrna_annotation_idx1 ON de_mrna_annotation USING btree (gpl_id, probe_id);

--
-- Name: de_mrna_annotation_idx2; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_mrna_annotation_idx2 ON de_mrna_annotation USING btree (gene_id, probeset_id);

--
-- Name: de_mrna_annotation_gpl_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_mrna_annotation
    ADD CONSTRAINT de_mrna_annotation_gpl_id_fk FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform) ON DELETE CASCADE;

