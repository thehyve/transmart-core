--
-- Name: bio_clinical_trial_uid(text, text, text); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION bio_clinical_trial_uid(trial_number text, title text, condition text) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN coalesce(TRIAL_NUMBER || '|', '') || coalesce(TITLE || '|', '') || coalesce(CONDITION, '');
END;
 
 
 
$$;

