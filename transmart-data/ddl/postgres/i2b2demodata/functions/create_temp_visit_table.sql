--
-- Name: create_temp_visit_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_visit_table(temptablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

BEGIN 
	-- Create temp table to store encounter/visit information
	execute 'create table ' ||  tempTableName || ' (
		encounter_id 			VARCHAR(200) not null,
		encounter_id_source 	VARCHAR(50) not null, 
		patient_id  			VARCHAR(200) not null,
		patient_id_source 		VARCHAR(50) not null,
		encounter_num	 		    NUMERIC(38,0), 
		inout_cd   			VARCHAR(50),
		location_cd 			VARCHAR(50),
		location_path 			VARCHAR(900),
 		start_date   			DATE, 
 		end_date    			DATE,
 		visit_blob 				TEXT,
 		update_date  			DATE,
		download_date 			DATE,
 		import_date 			DATE,
		sourcesystem_cd 		VARCHAR(50)
	)';

    execute 'CREATE INDEX idx_' || tempTableName || '_enc_id ON ' || tempTableName || '  ( encounter_id,encounter_id_source,patient_id,patient_id_source )';
    execute 'CREATE INDEX idx_' || tempTableName || '_patient_id ON ' || tempTableName || '  ( patient_id,patient_id_source )';
    
    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

