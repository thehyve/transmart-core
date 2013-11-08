--
-- Type: VIEW; Owner: BIOMART; Name: BIO_MARKER_CORREL_MV_VIEW
--
  CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_MARKER_CORREL_MV_VIEW" ("BIO_MARKER_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE", "MV_ID") AS 
  (        (        (        (         SELECT DISTINCT b.bio_marker_id, 
                                            b.bio_marker_id AS asso_bio_marker_id, 
                                            'GENE' AS correl_type, 
                                            1 AS mv_id
                                           FROM biomart.bio_marker b
                                          WHERE b.bio_marker_type = 'GENE'
                                UNION 
                                         SELECT DISTINCT b.bio_marker_id, 
                                            b.bio_marker_id AS asso_bio_marker_id, 
                                            'Protein' AS correl_type, 
                                            4 AS mv_id
                                           FROM biomart.bio_marker b
                                          WHERE b.bio_marker_type = 'Protein')
                        UNION 
                                 SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                                    c.asso_bio_data_id AS asso_bio_marker_id, 
                                    'PATHWAY_GENE' AS correl_type, 
                                    2 AS mv_id
                                   FROM biomart.bio_marker b, 
                                    biomart.bio_data_correlation c, 
                                    biomart.bio_data_correl_descr d
                                  WHERE b.bio_marker_id = c.bio_data_id AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id AND b.primary_source_code <> 'ARIADNE' AND d.correlation = 'PATHWAY GENE')
                UNION 
                         SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                            c.asso_bio_data_id AS asso_bio_marker_id, 
                            'HOMOLOGENE_GENE' AS correl_type, 3 AS mv_id
                           FROM biomart.bio_marker b, 
                            biomart.bio_data_correlation c, 
                            biomart.bio_data_correl_descr d
                          WHERE b.bio_marker_id = c.bio_data_id AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id AND d.correlation = 'HOMOLOGENE GENE')
        UNION 
                 SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                    c.asso_bio_data_id AS asso_bio_marker_id, 
                    'PROTEIN TO GENE' AS correl_type, 5 AS mv_id
                   FROM biomart.bio_marker b, biomart.bio_data_correlation c, 
                    biomart.bio_data_correl_descr d
                  WHERE b.bio_marker_id = c.bio_data_id AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id AND d.correlation = 'PROTEIN TO GENE')
UNION 
         SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
            c.asso_bio_data_id AS asso_bio_marker_id, 
            'GENE TO PROTEIN' AS correl_type, 6 AS mv_id
           FROM biomart.bio_marker b, biomart.bio_data_correlation c, 
            biomart.bio_data_correl_descr d
          WHERE b.bio_marker_id = c.bio_data_id AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id AND d.correlation = 'GENE TO PROTEIN';
 
