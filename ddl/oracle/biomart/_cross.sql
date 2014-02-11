--
-- Type: VIEW; Owner: BIOMART; Name: BIO_METAB_SUBPATHWAY_VIEW
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_METAB_SUBPATHWAY_VIEW" ("SUBPATHWAY_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE") AS 
SELECT
              SP.id,
              B.bio_marker_id,
              'SUBPATHWAY TO METABOLITE'
          FROM
              deapp.de_metabolite_sub_pathways SP
              INNER JOIN deapp.de_metabolite_sub_pway_metab J ON (SP.id = J.sub_pathway_id)
              INNER JOIN deapp.de_metabolite_annotation M ON (M.id = J.metabolite_id)
              INNER JOIN biomart.bio_marker B ON (
                  B.bio_marker_type = 'METABOLITE' AND
                  B.primary_external_id = M.hmdb_id);

--
-- Type: VIEW; Owner: BIOMART; Name: BIO_METAB_SUPERPATHWAY_VIEW
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_METAB_SUPERPATHWAY_VIEW" ("SUPERPATHWAY_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE") AS 
SELECT
              SUPP.id,
              B.bio_marker_id,
              'SUPERPATHWAY TO METABOLITE'
          FROM
              deapp.de_metabolite_super_pathways SUPP
              INNER JOIN deapp.de_metabolite_sub_pathways SUBP ON (SUPP.id = SUBP.super_pathway_id)
              INNER JOIN deapp.de_metabolite_sub_pway_metab J ON (SUBP.id = J.sub_pathway_id)
              INNER JOIN deapp.de_metabolite_annotation M ON (M.id = J.metabolite_id)
              INNER JOIN biomart.bio_marker B ON (
                  B.bio_marker_type = 'METABOLITE' AND
                  B.primary_external_id = M.hmdb_id);

