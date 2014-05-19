--
-- Name: bio_clinical_trial_uid(text, text, text); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION bio_clinical_trial_uid(trial_number character varying, title character varying, condition character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN coalesce(TRIAL_NUMBER || '|', '') || coalesce(TITLE || '|', '') || coalesce(CONDITION, '');
END;
 
 
 
$$;

