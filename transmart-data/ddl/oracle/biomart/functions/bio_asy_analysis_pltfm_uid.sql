--
-- Type: FUNCTION; Owner: BIOMART; Name: BIO_ASY_ANALYSIS_PLTFM_UID
--
  CREATE OR REPLACE FUNCTION "BIOMART"."BIO_ASY_ANALYSIS_PLTFM_UID" (
  PLATFORM_NAME VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAAP:' || nvl(PLATFORM_NAME, 'ERROR');
END bio_asy_analysis_pltfm_uid;
 
 
 
 
 
 
 
 
 
 
/
 
