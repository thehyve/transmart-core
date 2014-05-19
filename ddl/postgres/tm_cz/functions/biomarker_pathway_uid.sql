--
-- Name: biomarker_pathway_uid(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION biomarker_pathway_uid (
  P_SOURCE IN character varying ,
  PATHWAY_ID  IN character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'PATHWAY:'|| P_SOURCE || ':' || coalesce(PATHWAY_ID, 'ERROR');
END biomarker_pathway_uid;
 
$body$
LANGUAGE PLPGSQL;
