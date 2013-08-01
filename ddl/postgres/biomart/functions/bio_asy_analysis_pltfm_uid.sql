--
-- Name: bio_asy_analysis_pltfm_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_asy_analysis_pltfm_uid(platform_name text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END;
$_$;

