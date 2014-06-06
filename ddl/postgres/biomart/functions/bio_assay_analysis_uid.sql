--
-- Name: bio_assay_analysis_uid(bigint); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_assay_analysis_uid(analysis_id bigint) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'BAA:' || coalesce(ANALYSIS_ID, -1);
END;
$_$;

