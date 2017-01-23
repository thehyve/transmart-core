--
-- Name: search_bio_mkr_correl_fast_view; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_bio_mkr_correl_fast_view AS
    SELECT i.search_gene_signature_id AS domain_object_id, i.bio_marker_id AS asso_bio_marker_id, 'GENE_SIGNATURE_ITEM'::character varying(40) AS correl_type, CASE WHEN (i.fold_chg_metric IS NULL) THEN (1)::bigint ELSE i.fold_chg_metric END AS value_metric, 3 AS mv_id FROM search_gene_signature_item i, search_gene_signature gs WHERE ((i.search_gene_signature_id = gs.search_gene_signature_id) AND (gs.deleted_flag = false));

