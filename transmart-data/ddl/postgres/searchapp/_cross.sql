--
-- Name: pathway_genes; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW pathway_genes AS
    SELECT k_gene.search_keyword_id AS gene_keyword_id, k_pathway.search_keyword_id AS pathway_keyword_id, b.asso_bio_marker_id AS gene_biomarker_id FROM search_keyword k_pathway, biomart.bio_marker_correl_mv b, search_keyword k_gene WHERE (((((b.correl_type = 'PATHWAY_GENE'::text) AND (b.bio_marker_id = k_pathway.bio_data_id)) AND ((k_pathway.data_category)::text = 'PATHWAY'::text)) AND (b.asso_bio_marker_id = k_gene.bio_data_id)) AND ((k_gene.data_category)::text = 'GENE'::text));

--
-- Name: search_bio_mkr_correl_view; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_bio_mkr_correl_view AS
    SELECT a.domain_object_id, a.asso_bio_marker_id, a.correl_type, a.value_metric, a.mv_id FROM (SELECT i.search_gene_signature_id AS domain_object_id, i.bio_marker_id AS asso_bio_marker_id, 'GENE_SIGNATURE_ITEM'::text AS correl_type, CASE WHEN (i.fold_chg_metric IS NULL) THEN (1)::bigint ELSE i.fold_chg_metric END AS value_metric, 1 AS mv_id FROM search_gene_signature_item i, search_gene_signature gs WHERE (((i.search_gene_signature_id = gs.search_gene_signature_id) AND (gs.deleted_flag IS FALSE)) AND (i.bio_marker_id IS NOT NULL)) UNION ALL SELECT i.search_gene_signature_id AS domain_object_id, bada.bio_marker_id AS asso_bio_marker_id, 'GENE_SIGNATURE_ITEM'::text AS correl_type, CASE WHEN (i.fold_chg_metric IS NULL) THEN (1)::bigint ELSE i.fold_chg_metric END AS value_metric, 2 AS mv_id FROM search_gene_signature_item i, search_gene_signature gs, biomart.bio_assay_data_annotation bada WHERE ((((i.search_gene_signature_id = gs.search_gene_signature_id) AND (gs.deleted_flag IS FALSE)) AND (bada.bio_assay_feature_group_id = i.bio_assay_feature_group_id)) AND (i.bio_assay_feature_group_id IS NOT NULL))) a;

