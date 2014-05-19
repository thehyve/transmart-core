--
-- Name: bio_experiment_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_experiment_uid (
  PRIMARY_ID character varying
)  RETURNS character varying AS $body$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'EXP:' || coalesce(PRIMARY_ID, 'ERROR');
END bio_experiment_uid;
 
$body$
LANGUAGE PLPGSQL;
