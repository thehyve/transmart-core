--
-- Type: FUNCTION; Owner: TM_CZ; Name: BIO_ASY_ANALYSIS_PLTFM_UID
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."BIO_ASY_ANALYSIS_PLTFM_UID" (
  PLATFORM_NAME VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAAP:' || nvl(PLATFORM_NAME, 'ERROR');
END bio_asy_analysis_pltfm_uid;




/
 
