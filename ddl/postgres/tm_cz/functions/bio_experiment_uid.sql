--
-- Name: bio_experiment_uid(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION bio_experiment_uid(primary_id character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_experiment.

  RETURN 'EXP:' || coalesce(PRIMARY_ID, 'ERROR');
END bio_experiment_uid;
 
$_$;

