--
-- Name: bio_asy_analysis_pltfm_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_asy_analysis_pltfm_uid (
  PLATFORM_NAME character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END bio_asy_analysis_pltfm_uid;
 
$body$
LANGUAGE PLPGSQL;
