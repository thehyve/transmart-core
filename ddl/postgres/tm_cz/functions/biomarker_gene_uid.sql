--
-- Name: biomarker_gene_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION biomarker_gene_uid (
  GENE_ID text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'GENE:' || coalesce(GENE_ID, 'ERROR');
END biomarker_gene_uid;
 
$body$
LANGUAGE PLPGSQL;
