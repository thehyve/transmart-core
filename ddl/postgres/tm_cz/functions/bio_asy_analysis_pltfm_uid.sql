--
-- Name: bio_asy_analysis_pltfm_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_asy_analysis_pltfm_uid (
  PLATFORM_NAME text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END bio_asy_analysis_pltfm_uid;
 
$body$
LANGUAGE PLPGSQL;
