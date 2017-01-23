--
-- Type: VIEW; Owner: SEARCHAPP; Name: LISTSIG_GENES
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."LISTSIG_GENES" ("GENE_KEYWORD_ID", "LIST_KEYWORD_ID") AS 
  SELECT k_gsi.search_keyword_id AS gene_keyword_id,
  k_gs.search_keyword_id AS list_keyword_id
 FROM searchapp.search_keyword k_gs,
  searchapp.search_gene_signature gs,
  searchapp.search_gene_signature_item gsi,
  searchapp.search_keyword k_gsi
WHERE (((k_gs.bio_data_id = gs.search_gene_signature_id) AND (gs.search_gene_signature_id = gsi.search_gene_signature_id)) AND (gsi.bio_marker_id = k_gsi.bio_data_id));
 
