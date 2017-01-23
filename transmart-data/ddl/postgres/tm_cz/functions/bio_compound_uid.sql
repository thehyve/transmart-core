--
-- Name: bio_compound_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION bio_compound_uid(jnj_number character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Function to create compound_uid.

  RETURN 'COM:' || JNJ_NUMBER;
END BIO_COMPOUND_UID;
 
$_$;

