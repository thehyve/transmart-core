--
-- Name: biomarker_gene_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION biomarker_gene_uid (
  GENE_ID character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'GENE:' || coalesce(GENE_ID, 'ERROR');
END biomarker_gene_uid;
 
$body$
LANGUAGE PLPGSQL;
