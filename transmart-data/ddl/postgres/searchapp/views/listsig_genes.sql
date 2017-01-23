--
-- Name: listsig_genes; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW listsig_genes AS
    SELECT k_gsi.search_keyword_id AS gene_keyword_id, k_gs.search_keyword_id AS list_keyword_id FROM search_keyword k_gs, search_gene_signature gs, search_gene_signature_item gsi, search_keyword k_gsi WHERE (((k_gs.bio_data_id = gs.search_gene_signature_id) AND (gs.search_gene_signature_id = gsi.search_gene_signature_id)) AND (gsi.bio_marker_id = k_gsi.bio_data_id));

