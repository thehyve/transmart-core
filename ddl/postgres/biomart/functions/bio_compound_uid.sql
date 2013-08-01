--
-- Name: bio_compound_uid(text, text, text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_compound_uid(cas_registry text, jnj_number text, cnto_number text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Function to create compound_uid.

  RETURN 'COM:' || coalesce(CAS_REGISTRY, coalesce(JNJ_NUMBER, coalesce(CNTO_NUMBER, 'ERROR')));
END;
$_$;

