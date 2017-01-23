--
-- Type: FUNCTION; Owner: BIOMART; Name: BIO_ASSAY_ANALYSIS_UID
--
  CREATE OR REPLACE FUNCTION "BIOMART"."BIO_ASSAY_ANALYSIS_UID" (
  ANALYSIS_NAME VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAA:' || nvl(ANALYSIS_NAME, 'ERROR');
END bio_assay_analysis_uid;

 
 
 
 
 
 
 
 
 
 
/
 
