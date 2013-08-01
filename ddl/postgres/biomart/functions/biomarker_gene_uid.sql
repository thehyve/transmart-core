--
-- Name: biomarker_gene_uid(text); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION biomarker_gene_uid(gene_id text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'GENE:' || coalesce(GENE_ID, 'ERROR');
END;
$_$;

