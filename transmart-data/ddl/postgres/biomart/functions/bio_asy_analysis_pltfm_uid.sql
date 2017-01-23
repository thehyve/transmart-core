--
-- Name: bio_asy_analysis_pltfm_uid(character varying); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_asy_analysis_pltfm_uid(platform_name character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_asy_analysis_pltfm

  RETURN 'BAAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END;
$_$;

