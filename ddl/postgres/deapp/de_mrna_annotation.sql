--
-- Name: de_mrna_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_mrna_annotation (
    gpl_id character varying(100),
    probe_id character varying(100),
    gene_symbol character varying(100),
    probeset_id bigint,
    gene_id bigint,
    organism character varying(200)
);

--
-- Name: de_mrna_annotation_index1; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_mrna_annotation_index1 ON de_mrna_annotation USING btree (probeset_id);

