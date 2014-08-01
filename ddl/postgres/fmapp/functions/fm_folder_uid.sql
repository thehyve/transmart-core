--
-- Name: fm_folder_uid(numeric); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION fm_folder_uid(folder_id numeric) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
	BEGIN
		RETURN 'FOL:' || FOLDER_ID;
	END;
$$;

