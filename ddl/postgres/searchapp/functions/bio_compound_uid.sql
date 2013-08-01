--
-- Name: bio_compound_uid(text, text, text); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION bio_compound_uid(cas_registry text, jnj_number text, cnto_number text) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN coalesce(CAS_REGISTRY || '|', '') || coalesce(JNJ_NUMBER || '|', '') || coalesce(CNTO_NUMBER, '');
END;
 
 
 
 
 
$$;

