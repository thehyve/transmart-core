--
-- Name: biomarker_pathway_uid(text, text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION biomarker_pathway_uid(p_source text, pathway_id text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'PATHWAY:'|| P_SOURCE || ':' || coalesce(PATHWAY_ID, 'ERROR');
END;
$_$;

