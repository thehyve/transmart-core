--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_BIO_MKR_CORREL_FST_VIEW
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_BIO_MKR_CORREL_FST_VIEW" ("DOMAIN_OBJECT_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE", "VALUE_METRIC", "MV_ID") AS 
  SELECT i.search_gene_signature_id AS domain_object_id,
  i.bio_marker_id AS asso_bio_marker_id,
  'GENE_SIGNATURE_ITEM' AS correl_type,
      CASE
          WHEN (i.fold_chg_metric IS NULL) THEN 1
          ELSE i.fold_chg_metric
      END AS value_metric,
  3 AS mv_id
 FROM searchapp.search_gene_signature_item i,
  searchapp.search_gene_signature gs
WHERE ((i.search_gene_signature_id = gs.search_gene_signature_id) AND (gs.deleted_flag = 0));
 
