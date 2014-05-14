--
-- Name: bio_assay_platform_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_assay_platform_uid (
  PLATFORM_NAME text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END bio_assay_platform_uid;

$body$
LANGUAGE PLPGSQL;
