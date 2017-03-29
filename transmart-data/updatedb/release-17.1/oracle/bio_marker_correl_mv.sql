DROP MATERIALIZED VIEW "BIOMART"."BIO_MARKER_CORREL_MV";
CREATE MATERIALIZED VIEW "BIOMART"."BIO_MARKER_CORREL_MV" ("BIO_MARKER_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE", "MV_ID")
 AS SELECT DISTINCT b.bio_marker_id,
   b.bio_marker_id AS asso_bio_marker_id,
   'GENE'          AS correl_type,
   1               AS mv_id
 FROM biomart.bio_marker b
 WHERE b.bio_marker_type = 'GENE'
 UNION
 SELECT DISTINCT b.bio_marker_id,
   b.bio_marker_id AS asso_bio_marker_id,
   'PROTEIN'       AS correl_type,
   4               AS mv_id
 FROM biomart.bio_marker b
 WHERE b.bio_marker_type = 'PROTEIN'
 UNION
 SELECT DISTINCT b.bio_marker_id,
   b.bio_marker_id AS asso_bio_marker_id,
   'MIRNA'         AS correl_type,
   7               AS mv_id
 FROM biomart.bio_marker b
 WHERE b.bio_marker_type = 'MIRNA'
 UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
   c.asso_bio_data_id          AS asso_bio_marker_id,
   'PATHWAY GENE'              AS correl_type,
   2                           AS mv_id
 FROM biomart.bio_marker b,
   biomart.bio_data_correlation c,
   biomart.bio_data_correl_descr d
 WHERE b.bio_marker_id          = c.bio_data_id
 AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
 AND b.primary_source_code     <> 'ARIADNE'
 AND d.correlation              = 'PATHWAY GENE'
 UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
   c.asso_bio_data_id          AS asso_bio_marker_id,
   'HOMOLOGENE_GENE'           AS correl_type,
   3                           AS mv_id
 FROM biomart.bio_marker b,
   biomart.bio_data_correlation c,
   biomart.bio_data_correl_descr d
 WHERE b.bio_marker_id          = c.bio_data_id
 AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
 AND d.correlation              = 'HOMOLOGENE GENE'
 UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
   c.asso_bio_data_id          AS asso_bio_marker_id,
   'PROTEIN TO GENE'           AS correl_type,
   5                           AS mv_id
 FROM biomart.bio_marker b,
   biomart.bio_data_correlation c,
   biomart.bio_data_correl_descr d
 WHERE b.bio_marker_id          = c.bio_data_id
 AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
 AND d.correlation              = 'PROTEIN TO GENE'
 UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
   c.asso_bio_data_id          AS asso_bio_marker_id,
   'GENE TO PROTEIN'           AS correl_type,
   6                           AS mv_id
 FROM biomart.bio_marker b,
   biomart.bio_data_correlation c,
   biomart.bio_data_correl_descr d
 WHERE b.bio_marker_id          = c.bio_data_id
 AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
 AND d.correlation              = 'GENE TO PROTEIN'
 UNION
 SELECT c1.bio_data_id,
   c2.asso_bio_data_id,
   'PATHWAY TO PROTEIN' AS correl_type,
   8                    AS mv_id
 FROM bio_data_correlation c1
 INNER JOIN bio_data_correlation c2
 ON c1.asso_bio_data_id = c2.bio_data_id
 INNER JOIN bio_data_correl_descr d1
 ON c1.bio_data_correl_descr_id = d1.bio_data_correl_descr_id
 INNER JOIN bio_data_correl_descr d2
 ON c2.bio_data_correl_descr_id = d2.bio_data_correl_descr_id
 WHERE d1.correlation           = 'PATHWAY GENE'
 AND d2.correlation             = 'GENE TO PROTEIN'
 UNION
 SELECT DISTINCT b.bio_marker_id,
   b.bio_marker_id AS asso_bio_marker_id,
   'METABOLITE'    AS correl_type,
   9               AS mv_id
 FROM biomart.bio_marker b
 WHERE b.bio_marker_type = 'METABOLITE'
 UNION
 SELECT DISTINCT b.bio_marker_id,
   b.bio_marker_id AS asso_bio_marker_id,
   'TRANSCRIPT'    AS correl_type,
   10              AS mv_id
 FROM biomart.bio_marker b
 WHERE b.bio_marker_type = 'TRANSCRIPT'
  UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
   c.asso_bio_data_id          AS asso_bio_marker_id,
   'GENE TO TRANSCRIPT'           AS correl_type,
   11                           AS mv_id
 FROM biomart.bio_marker b,
   biomart.bio_data_correlation c,
   biomart.bio_data_correl_descr d
 WHERE b.bio_marker_id          = c.bio_data_id
 AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
 AND d.correlation              = 'GENE TO TRANSCRIPT';
 
GRANT SELECT ON "BIOMART"."BIO_MARKER_CORREL_MV" TO "BIOMART_USER";
GRANT ALL ON "BIOMART"."BIO_MARKER_CORREL_MV" TO "BIOMART";
