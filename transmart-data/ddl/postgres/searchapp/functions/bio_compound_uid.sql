--
-- Name: bio_compound_uid(character varying, character varying, character varying); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION bio_compound_uid(cas_registry character varying, jnj_number character varying, cnto_number character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN coalesce(CAS_REGISTRY || '|', '') || coalesce(JNJ_NUMBER || '|', '') || coalesce(CNTO_NUMBER, '');
END;
 
 
 
 
 
$$;

