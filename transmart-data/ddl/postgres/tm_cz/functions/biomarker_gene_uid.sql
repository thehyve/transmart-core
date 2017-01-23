--
-- Name: biomarker_gene_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION biomarker_gene_uid(gene_id character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for biomarker_gene.

  RETURN 'GENE:' || coalesce(GENE_ID, 'ERROR');
END biomarker_gene_uid;
 
$_$;

