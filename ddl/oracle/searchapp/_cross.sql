--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_BIO_MKR_CORREL_VIEW
--
CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_BIO_MKR_CORREL_VIEW" ("DOMAIN_OBJECT_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE", "VALUE_METRIC", "MV_ID") AS 
SELECT domain_object_id,
  asso_bio_marker_id,
  correl_type,
  value_metric,
  mv_id
FROM
  (SELECT i.SEARCH_GENE_SIGNATURE_ID AS domain_object_id,
    i.BIO_MARKER_ID                  AS asso_bio_marker_id,
    'GENE_SIGNATURE_ITEM'            AS correl_type,
    CASE
      WHEN i.FOLD_CHG_METRIC IS NULL
      THEN 1
      ELSE i.FOLD_CHG_METRIC
    END AS value_metric,
    1   AS mv_id
  FROM SEARCH_GENE_SIGNATURE_ITEM i,
    SEARCH_GENE_SIGNATURE gs
  WHERE i.SEARCH_GENE_SIGNATURE_ID = gs.SEARCH_GENE_SIGNATURE_ID
  AND gs.DELETED_FLAG              = 0
  AND i.bio_marker_id             IS NOT NULL
  UNION ALL
  SELECT i.SEARCH_GENE_SIGNATURE_ID AS domain_object_id,
    bada.BIO_MARKER_ID              AS asso_bio_marker_id,
    'GENE_SIGNATURE_ITEM'           AS correl_type,
    CASE
      WHEN i.FOLD_CHG_METRIC IS NULL
      THEN 1
      ELSE i.FOLD_CHG_METRIC
    END AS value_metric,
    2   AS mv_id
  FROM SEARCH_GENE_SIGNATURE_ITEM i,
    SEARCH_GENE_SIGNATURE gs,
    biomart.bio_assay_data_annotation bada
  WHERE i.SEARCH_GENE_SIGNATURE_ID    = gs.SEARCH_GENE_SIGNATURE_ID
  AND gs.DELETED_FLAG                 = 0
  AND bada.bio_assay_feature_group_id = i.bio_assay_feature_group_id
  AND i.bio_assay_feature_group_id   IS NOT NULL
  );
