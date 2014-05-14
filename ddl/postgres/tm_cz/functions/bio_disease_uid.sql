--
-- Name: bio_disease_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_disease_uid (
  MESH_CODE text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates bio_disease_uid.

  RETURN 'DIS:' || coalesce(MESH_CODE, 'ERROR');
END BIO_DISEASE_UID;
 
$body$
LANGUAGE PLPGSQL;
