--
-- Name: bio_assay_analysis_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_assay_analysis_uid (
  ANALYSIS_NAME character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAA:' || coalesce(ANALYSIS_NAME, 'ERROR');
END bio_assay_analysis_uid;

$body$
LANGUAGE PLPGSQL;
