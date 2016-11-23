--
-- Name: bio_jub_oncol_data_uid(numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION bio_jub_oncol_data_uid(record_id numeric, bio_curation_name character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_jub_oncol_data.

  RETURN 'BJOD:' || coalesce(TO_CHAR(RECORD_ID), 'ERROR') || ':' || coalesce(BIO_CURATION_NAME, 'ERROR');
END BIO_JUB_ONCOL_DATA_UID;
 
$_$;

