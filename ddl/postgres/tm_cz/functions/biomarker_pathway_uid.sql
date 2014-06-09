--
-- Name: biomarker_pathway_uid(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION biomarker_pathway_uid(p_source character varying, pathway_id character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for biomarker_pathway.

  RETURN 'PATHWAY:'|| P_SOURCE || ':' || coalesce(PATHWAY_ID, 'ERROR');
END biomarker_pathway_uid;
 
$_$;

