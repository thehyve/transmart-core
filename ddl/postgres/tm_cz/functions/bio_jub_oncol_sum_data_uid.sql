--
-- Name: bio_jub_oncol_sum_data_uid(bigint, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_jub_oncol_sum_data_uid (
  RECORD_ID bigint,
  BIO_CURATION_NAME text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BJOS:' || coalesce(TO_CHAR(RECORD_ID), 'ERROR') || ':' || coalesce(BIO_CURATION_NAME, 'ERROR');
END BIO_JUB_ONCOL_SUM_DATA_UID;
 
$body$
LANGUAGE PLPGSQL;
