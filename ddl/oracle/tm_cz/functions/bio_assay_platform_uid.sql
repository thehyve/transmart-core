--
-- Type: FUNCTION; Owner: TM_CZ; Name: BIO_ASSAY_PLATFORM_UID
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."BIO_ASSAY_PLATFORM_UID" (
  PLATFORM_NAME VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAP:' || nvl(PLATFORM_NAME, 'ERROR');
END bio_assay_platform_uid;




/
 
