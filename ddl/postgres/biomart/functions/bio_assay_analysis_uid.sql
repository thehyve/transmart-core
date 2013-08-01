--
-- Name: bio_assay_analysis_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_assay_analysis_uid(analysis_name text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAA:' || coalesce(ANALYSIS_NAME, 'ERROR');
END;
$_$;

