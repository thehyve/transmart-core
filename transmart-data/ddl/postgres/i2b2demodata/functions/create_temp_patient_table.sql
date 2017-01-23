--
-- Name: create_temp_patient_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_patient_table(temppatientdimensiontablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

BEGIN 
	-- Create temp table to store encounter/visit information
	execute 'create table ' ||  tempPatientDimensionTableName || ' (
		PATIENT_ID VARCHAR(200), 
		PATIENT_ID_SOURCE VARCHAR(50),
		PATIENT_NUM NUMERIC(38,0),
	    VITAL_STATUS_CD VARCHAR(50), 
	    BIRTH_DATE DATE, 
	    DEATH_DATE DATE, 
	    SEX_CD CHAR(50), 
	    AGE_IN_YEARS_NUM NUMERIC(5,0), 
	    LANGUAGE_CD VARCHAR(50), 
		RACE_CD VARCHAR(50 ), 
		MARITAL_STATUS_CD VARCHAR(50), 
		RELIGION_CD VARCHAR(50), 
		ZIP_CD VARCHAR(50), 
		STATECITYZIP_PATH VARCHAR(700), 
		PATIENT_BLOB TEXT, 
		UPDATE_DATE DATE, 
		DOWNLOAD_DATE DATE, 
		IMPORT_DATE DATE, 
		SOURCESYSTEM_CD VARCHAR(50)
	)';

execute 'CREATE INDEX idx_' || tempPatientDimensionTableName || '_pat_id ON ' || tempPatientDimensionTableName || '  (PATIENT_ID, PATIENT_ID_SOURCE,PATIENT_NUM)';
  
     
    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

