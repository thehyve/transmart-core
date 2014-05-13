--
-- Name: fm_folder_uid(text); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION fm_folder_uid(folder_name text) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for fm_folder.

--  RETURN 'FOL:' || coalesce(FOLDER_NAME, 'ERROR');
  RETURN 'FOL:' || FOLDER_NAME;
END;
$_$;
