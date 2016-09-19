--
-- Name: remove_temp_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION remove_temp_table(temptablename character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN 
	execute 'drop table ' || tempTableName || ' cascade';
	
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;
$$;

