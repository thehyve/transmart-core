--
-- Name: bio_disease_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_disease_uid(mesh_code text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates bio_disease_uid.

  RETURN 'DIS:' || coalesce(MESH_CODE, 'ERROR');
END;
$_$;

