--
-- Type: FUNCTION; Owner: TM_CZ; Name: BIO_EXPERIMENT_UID
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."BIO_EXPERIMENT_UID" (
  PRIMARY_ID VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'EXP:' || nvl(PRIMARY_ID, 'ERROR');
END bio_experiment_uid;




/
 
