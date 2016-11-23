--
-- Name: fm_file_uid(character varying); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION fm_file_uid(file_id character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for fm_file.

  RETURN 'FIL:' || coalesce(FILE_ID, 'ERROR');
END;
$_$;

