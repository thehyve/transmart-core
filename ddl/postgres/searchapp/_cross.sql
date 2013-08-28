--
-- Name: pathway_genes; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW pathway_genes AS
 SELECT k_gene.search_keyword_id AS gene_keyword_id, 
    k_pathway.search_keyword_id AS pathway_keyword_id, 
    b.asso_bio_marker_id AS gene_biomarker_id
   FROM search_keyword k_pathway, 
    biomart.bio_marker_correl_mv b, 
    search_keyword k_gene
  WHERE (((((b.correl_type = 'PATHWAY_GENE'::text) AND (b.bio_marker_id = k_pathway.bio_data_id)) AND ((k_pathway.data_category)::text = 'PATHWAY'::text)) AND (b.asso_bio_marker_id = k_gene.bio_data_id)) AND ((k_gene.data_category)::text = 'GENE'::text));

