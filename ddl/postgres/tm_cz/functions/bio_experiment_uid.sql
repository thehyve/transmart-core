--
-- Name: bio_experiment_uid(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_experiment_uid (
  PRIMARY_ID text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'EXP:' || coalesce(PRIMARY_ID, 'ERROR');
END bio_experiment_uid;
 
$body$
LANGUAGE PLPGSQL;
