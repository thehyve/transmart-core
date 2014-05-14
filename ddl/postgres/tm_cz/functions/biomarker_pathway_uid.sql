--
-- Name: biomarker_pathway_uid(text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION biomarker_pathway_uid (
  P_SOURCE IN text ,
  PATHWAY_ID  IN text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'PATHWAY:'|| P_SOURCE || ':' || coalesce(PATHWAY_ID, 'ERROR');
END biomarker_pathway_uid;
 
$body$
LANGUAGE PLPGSQL;
