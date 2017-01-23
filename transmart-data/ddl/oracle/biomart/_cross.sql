--
-- Type: VIEW; Owner: BIOMART; Name: BIO_METAB_SUBPATHWAY_VIEW
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_METAB_SUBPATHWAY_VIEW" ("SUBPATHWAY_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE") AS 
SELECT SP.id,
  B.bio_marker_id,
  'SUBPATHWAY TO METABOLITE'
FROM deapp.de_metabolite_sub_pathways SP
INNER JOIN deapp.de_metabolite_sub_pway_metab J
ON (SP.id = J.sub_pathway_id)
INNER JOIN deapp.de_metabolite_annotation M
ON (M.id = J.metabolite_id)
INNER JOIN biomart.bio_marker B
ON ( B.bio_marker_type    = 'METABOLITE'
AND B.primary_external_id = M.hmdb_id);
 
--
-- Type: TRIGGER; Owner: BIOMART; Name: TRG_BIO_ANALYSIS_ATT_BAAL
--
  CREATE OR REPLACE TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATT_BAAL" 
  after insert on "BIOMART"."BIO_ANALYSIS_ATTRIBUTE"
  for each row
declare
  -- local variables here
begin
  if inserting then
    -- create a new record in the lineage table for each ancestor of this term (including self)
    insert into BIO_ANALYSIS_ATTRIBUTE_LINEAGE
    (BIO_ANALYSIS_ATTRIBUTE_ID, ANCESTOR_TERM_ID, ANCESTOR_SEARCH_KEYWORD_ID)
    SELECT :NEW.BIO_ANALYSIS_ATTRIBUTE_ID, skl.ancestor_id, skl.search_keyword_id
    FROM searchapp.solr_keywords_lineage skl
    WHERE skl.term_id = :NEW.TERM_ID;
  end if;

end TRG_BIO_ANALYSIS_ATT_BAAL;


/
ALTER TRIGGER "BIOMART"."TRG_BIO_ANALYSIS_ATT_BAAL" DISABLE;
--
-- Type: VIEW; Owner: BIOMART; Name: BIO_MARKER_EXP_ANALYSIS_MV
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_MARKER_EXP_ANALYSIS_MV" ("BIO_MARKER_ID", "BIO_EXPERIMENT_ID", "BIO_ASSAY_ANALYSIS_ID", "MV_ID") AS 
SELECT DISTINCT t4.bio_marker_id,
  t1.bio_experiment_id,
  t1.bio_assay_analysis_id,
  t1.bio_assay_analysis_id*100+t4.bio_marker_id
FROM BIO_ASSAY_ANALYSIS_DATA t1,
  BIO_EXPERIMENT t2,
  DEAPP.DE_MRNA_ANNOTATION t3,
  BIO_MARKER t4
WHERE t1.bio_experiment_id = t2.bio_experiment_id
AND t2.bio_experiment_type ='Experiment'
AND t3.probeset_id         = t1.probeset_id
AND t4.primary_external_id = CAST(t3.gene_id AS VARCHAR(200));
 
--
-- Type: VIEW; Owner: BIOMART; Name: BIO_METAB_SUPERPATHWAY_VIEW
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_METAB_SUPERPATHWAY_VIEW" ("SUPERPATHWAY_ID", "ASSO_BIO_MARKER_ID", "CORREL_TYPE") AS 
SELECT SUPP.id,
  B.bio_marker_id,
  'SUPERPATHWAY TO METABOLITE'
FROM deapp.de_metabolite_super_pathways SUPP
INNER JOIN deapp.de_metabolite_sub_pathways SUBP
ON (SUPP.id = SUBP.super_pathway_id)
INNER JOIN deapp.de_metabolite_sub_pway_metab J
ON (SUBP.id = J.sub_pathway_id)
INNER JOIN deapp.de_metabolite_annotation M
ON (M.id = J.metabolite_id)
INNER JOIN biomart.bio_marker B
ON ( B.bio_marker_type    = 'METABOLITE'
AND B.primary_external_id = M.hmdb_id);
 
