--
-- Name: create_temp_provider_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_provider_table(tempprovidertablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

BEGIN 

execute 'create table ' ||  tempProviderTableName || ' (
    PROVIDER_ID VARCHAR(50) NOT NULL, 
	PROVIDER_PATH VARCHAR(700) NOT NULL, 
	NAME_CHAR VARCHAR(2000), 
	PROVIDER_BLOB TEXT, 
	UPDATE_DATE DATE, 
	DOWNLOAD_DATE DATE, 
	IMPORT_DATE DATE, 
	SOURCESYSTEM_CD VARCHAR(50), 
	UPLOAD_ID NUMERIC(*,0)
	 )';
 execute 'CREATE INDEX idx_' || tempProviderTableName || '_ppath_id ON ' || tempProviderTableName || '  (PROVIDER_PATH)';

    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

