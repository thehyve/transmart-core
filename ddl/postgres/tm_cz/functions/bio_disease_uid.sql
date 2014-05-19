--
-- Name: bio_disease_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_disease_uid (
  MESH_CODE character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates bio_disease_uid.

  RETURN 'DIS:' || coalesce(MESH_CODE, 'ERROR');
END BIO_DISEASE_UID;
 
$body$
LANGUAGE PLPGSQL;
