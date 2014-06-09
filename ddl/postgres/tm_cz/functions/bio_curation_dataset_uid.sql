--
-- Name: bio_curation_dataset_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION bio_curation_dataset_uid(bio_curation_type character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_curation_dataset.

  RETURN 'BCD:' || coalesce(BIO_CURATION_TYPE, 'ERROR');
END BIO_CURATION_DATASET_UID;
 
$_$;

