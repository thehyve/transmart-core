--
-- Type: VIEW; Owner: SEARCHAPP; Name: PATHWAY_GENES
--
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "SEARCHAPP"."PATHWAY_GENES" ("GENE_KEYWORD_ID", "PATHWAY_KEYWORD_ID", "GENE_BIOMARKER_ID") AS 
  SELECT k_gene.search_keyword_id AS gene_keyword_id,
  k_pathway.search_keyword_id AS pathway_keyword_id,
  b.asso_bio_marker_id AS gene_biomarker_id
 FROM searchapp.search_keyword k_pathway,
  biomart.bio_marker_correl_mv b,
  searchapp.search_keyword k_gene
WHERE ((((((b.correl_type) = 'PATHWAY_GENE') AND (b.bio_marker_id = k_pathway.bio_data_id)) AND ((k_pathway.data_category) = 'PATHWAY')) AND (b.asso_bio_marker_id = k_gene.bio_data_id)) AND ((k_gene.data_category) = 'GENE'));
