--
-- Name: gene_ontology_data; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE gene_ontology_data (
    pathway character varying(100),
    gene_id character varying(20),
    gene_symbol character varying(200),
    organism character varying(100)
);

--
-- Name: idx_god_organism; Type: INDEX; Schema: biomart_user; Owner: -
--
CREATE INDEX idx_god_organism ON gene_ontology_data USING btree (organism);

--
-- Name: idx_god_symbol; Type: INDEX; Schema: biomart_user; Owner: -
--
CREATE INDEX idx_god_symbol ON gene_ontology_data USING btree (gene_symbol);

