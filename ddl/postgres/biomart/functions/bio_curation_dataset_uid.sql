--
-- Name: bio_curation_dataset_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_curation_dataset_uid(bio_curation_type text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BCD:' || coalesce(BIO_CURATION_TYPE, 'ERROR');
END;
$_$;

