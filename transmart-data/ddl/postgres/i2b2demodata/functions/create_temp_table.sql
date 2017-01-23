--
-- Name: create_temp_table(character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION create_temp_table(temptablename character varying, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

BEGIN 
	execute 'create table ' ||  tempTableName || '  (		
		encounter_id 		varchar(200) not null, 
        encounter_id_source varchar(50) not null,
		encounter_num  		NUMERIC(38,0),
		concept_cd 	 		VARCHAR(50) not null, 
        patient_num 		NUMERIC(38,0), 
		patient_id  		varchar(200) not null,
        patient_id_source  	varchar(50) not null,
		provider_id   		VARCHAR(50),
 		start_date   		DATE, 
		modifier_cd 		VARCHAR(100),
	    instance_num 		NUMERIC(18,0),
 		valtype_cd 			VARCHAR(50),
		tval_char 			varchar(255),
 		nval_num 			NUMERIC(18,5),
		valueflag_cd 		CHAR(50),
 		quantity_num 		NUMERIC(18,5),
		confidence_num 		NUMERIC(18,0),
 		observation_blob 	TEXT,
		units_cd 			VARCHAR(50),
 		end_date    		DATE,
		location_cd 		VARCHAR(50),
 		update_date  		DATE,
		download_date 		DATE,
 		import_date 		DATE,
		sourcesystem_cd 	VARCHAR(50) ,
 		upload_id 			INTEGER
	)';

    
    execute 'CREATE INDEX idx_' || tempTableName || '_pk ON ' || tempTableName || '  ( encounter_num,patient_num,concept_cd,provider_id,start_date,modifier_cd,instance_num)';
    execute 'CREATE INDEX idx_' || tempTableName || '_enc_pat_id ON ' || tempTableName || '  (encounter_id,encounter_id_source, patient_id,patient_id_source )';
    
    
EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '% - %', SQLSTATE, SQLERRM;
END;

$$;

