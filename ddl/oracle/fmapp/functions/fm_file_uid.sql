--
-- Type: FUNCTION; Owner: FMAPP; Name: FM_FILE_UID
--
  CREATE OR REPLACE FUNCTION "FMAPP"."FM_FILE_UID" (
  FILE_ID NUMBER
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_concept_code.

  RETURN 'FIL:' || FILE_ID;
END FM_FILE_UID;
/
 
