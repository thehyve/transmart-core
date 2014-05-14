--
-- Name: bio_curation_dataset_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_curation_dataset_uid (
  BIO_CURATION_TYPE text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BCD:' || coalesce(BIO_CURATION_TYPE, 'ERROR');
END BIO_CURATION_DATASET_UID;
 
$body$
LANGUAGE PLPGSQL;
