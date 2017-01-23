--
-- Type: FUNCTION; Owner: BIOMART; Name: BIO_ASSAY_PLATFORM_UID
--
  CREATE OR REPLACE FUNCTION "BIOMART"."BIO_ASSAY_PLATFORM_UID" (
  PLATFORM_NAME VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAP:' || nvl(PLATFORM_NAME, 'ERROR');
END bio_assay_platform_uid;

 
 
 
 
 
 
 
 
 
 
/
 
