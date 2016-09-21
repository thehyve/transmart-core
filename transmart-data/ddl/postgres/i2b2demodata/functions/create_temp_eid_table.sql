--
-- Name: create_temp_eid_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_eid_table(temppatientmappingtablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$

BEGIN 
execute 'create table ' ||  tempPatientMappingTableName || ' (
	ENCOUNTER_MAP_ID       	VARCHAR(200) NOT NULL,
    ENCOUNTER_MAP_ID_SOURCE	VARCHAR(50) NOT NULL,
    PATIENT_MAP_ID          VARCHAR(200), 
	PATIENT_MAP_ID_SOURCE   VARCHAR(50), 
    ENCOUNTER_ID       	    VARCHAR(200) NOT NULL,
    ENCOUNTER_ID_SOURCE     VARCHAR(50) ,
    ENCOUNTER_NUM           NUMERIC, 
    ENCOUNTER_MAP_ID_STATUS    VARCHAR(50),
    PROCESS_STATUS_FLAG     CHAR(1),
	UPDATE_DATE DATE, 
	DOWNLOAD_DATE DATE, 
	IMPORT_DATE DATE, 
	SOURCESYSTEM_CD VARCHAR(50)
)';

execute 'CREATE INDEX idx_' || tempPatientMappingTableName || '_eid_id ON ' || tempPatientMappingTableName || '  (ENCOUNTER_ID, ENCOUNTER_ID_SOURCE, ENCOUNTER_MAP_ID, ENCOUNTER_MAP_ID_SOURCE, ENCOUNTER_NUM)';

 execute 'CREATE INDEX idx_' || tempPatientMappingTableName || '_stateid_eid_id ON ' || tempPatientMappingTableName || '  (PROCESS_STATUS_FLAG)';  
    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

