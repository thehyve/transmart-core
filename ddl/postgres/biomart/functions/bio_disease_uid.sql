--
-- Name: bio_disease_uid(character varying); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_disease_uid(mesh_code character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates bio_disease_uid.

  RETURN 'DIS:' || coalesce(MESH_CODE, 'ERROR');
END;
$_$;

