--
-- Type: FUNCTION; Owner: BIOMART; Name: BIO_DISEASE_UID
--
  CREATE OR REPLACE FUNCTION "BIOMART"."BIO_DISEASE_UID" (
  MESH_CODE VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates bio_disease_uid.

  RETURN 'DIS:' || nvl(MESH_CODE, 'ERROR');
END BIO_DISEASE_UID;
 
 
 
 
 
 
 
 
 
 
 
/
 
