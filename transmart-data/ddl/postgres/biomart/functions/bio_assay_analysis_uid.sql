--
-- Name: bio_assay_analysis_uid(character varying); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_assay_analysis_uid(analysis_name character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAA:' || coalesce(ANALYSIS_NAME, 'ERROR');
END;
$_$;

