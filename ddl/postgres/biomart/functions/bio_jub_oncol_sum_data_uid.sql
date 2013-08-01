--
-- Name: bio_jub_oncol_sum_data_uid(bigint, text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_jub_oncol_sum_data_uid(record_id bigint, bio_curation_name text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BJOS:' || coalesce(trim(TO_CHAR(RECORD_ID, '9999999999999999999')), 'ERROR') || ':' || coalesce(BIO_CURATION_NAME, 'ERROR');
END;
$_$;

