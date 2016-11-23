--
-- Type: FUNCTION; Owner: TM_CZ; Name: BIOMARKER_PATHWAY_UID
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."BIOMARKER_PATHWAY_UID" (
  P_SOURCE IN VARCHAR2 ,
  PATHWAY_ID  IN VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'PATHWAY:'|| P_SOURCE || ':' || nvl(PATHWAY_ID, 'ERROR');
END biomarker_pathway_uid;




/
 
