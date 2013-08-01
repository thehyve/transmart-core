--
-- Name: create_temp_pid_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_pid_table(temppatientmappingtablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$

BEGIN 
execute 'create table ' ||  tempPatientMappingTableName || ' (
	   	PATIENT_MAP_ID VARCHAR(200), 
		PATIENT_MAP_ID_SOURCE VARCHAR(50), 
		PATIENT_ID_STATUS VARCHAR(50), 
		PATIENT_ID  VARCHAR(200),
	    PATIENT_ID_SOURCE varchar(50),
		PATIENT_NUM NUMERIC(38,0),
	    PATIENT_MAP_ID_STATUS VARCHAR(50), 
		PROCESS_STATUS_FLAG CHAR(1), 
		UPDATE_DATE DATE, 
		DOWNLOAD_DATE DATE, 
		IMPORT_DATE DATE, 
		SOURCESYSTEM_CD VARCHAR(50)

	 )';

execute 'CREATE INDEX idx_' || tempPatientMappingTableName || '_pid_id ON ' || tempPatientMappingTableName || '  ( PATIENT_ID, PATIENT_ID_SOURCE )';

execute 'CREATE INDEX idx_' || tempPatientMappingTableName || 'map_pid_id ON ' || tempPatientMappingTableName || '  
( PATIENT_ID, PATIENT_ID_SOURCE,PATIENT_MAP_ID, PATIENT_MAP_ID_SOURCE,  PATIENT_NUM )';
 
execute 'CREATE INDEX idx_' || tempPatientMappingTableName || 'stat_pid_id ON ' || tempPatientMappingTableName || '  
(PROCESS_STATUS_FLAG)';


    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

