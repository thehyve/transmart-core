--
-- Name: bio_assay_platform_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_assay_platform_uid(platform_name text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END;
$_$;

