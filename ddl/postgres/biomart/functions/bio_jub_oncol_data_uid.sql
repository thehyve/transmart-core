--
-- Name: bio_jub_oncol_data_uid(numeric, character varying); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_jub_oncol_data_uid(record_id numeric, bio_curation_name character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_jub_oncol_data.

  RETURN 'BJOD:' || coalesce(trim(TO_CHAR(RECORD_ID, '9999999999999999999')), 'ERROR') || ':' || coalesce(BIO_CURATION_NAME, 'ERROR');
END;
$_$;

