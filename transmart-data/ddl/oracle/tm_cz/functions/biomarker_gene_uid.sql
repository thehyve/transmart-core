--
-- Type: FUNCTION; Owner: TM_CZ; Name: BIOMARKER_GENE_UID
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."BIOMARKER_GENE_UID" (
  GENE_ID VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'GENE:' || nvl(GENE_ID, 'ERROR');
END biomarker_gene_uid;




/
 
