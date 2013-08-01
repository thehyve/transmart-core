--
-- Name: sync_clear_provider_table(character varying, character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION sync_clear_provider_table(tempprovidertablename character varying, backupprovidertablename character varying, uploadid numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ DECLARE 

DECLARE
interProviderTableName  varchar(400);

BEGIN 
	interProviderTableName := backupProviderTableName || '_inter';
	
	--Delete duplicate rows with same encounter and patient combination
	-- smuniraju: rowid not supported in postgres	
	-- execute 'DELETE FROM ' || tempProviderTableName || ' t1 WHERE rowid > 
	-- 				   (SELECT  min(rowid) FROM ' || tempProviderTableName || ' t2
	-- 				     WHERE t1.provider_id = t2.provider_id 
    --                                        AND t1.provider_path = t2.provider_path
    --                                         )';
	execute 'DELETE FROM ' || tempProviderTableName || ' t1 
			 WHERE ( ctid) NOT IN (
				SELECT  max(ctid) 
				FROM ' || tempProviderTableName || ' t2
				GROUP BY   provider_path,provider_id)';				
											
    execute 'create table ' ||  interProviderTableName || ' (
    PROVIDER_ID         VARCHAR(50) NOT NULL,
	PROVIDER_PATH       VARCHAR(700) NOT NULL,
	NAME_CHAR       	VARCHAR(850) NULL,
	PROVIDER_BLOB       TEXT NULL,
	UPDATE_DATE     	DATE NULL,
	DOWNLOAD_DATE       DATE NULL,
	IMPORT_DATE         DATE NULL,
	SOURCESYSTEM_CD     VARCHAR(50) NULL,
	UPLOAD_ID        	NUMERIC(38,0) NULL ,
    CONSTRAINT  ' || interProviderTableName || '_pk PRIMARY KEY(PROVIDER_PATH,provider_id)
	 )';
    
    --Create new patient(patient_mapping) if temp table patient_ide does not exists 
	-- in patient_mapping table.
	execute 'insert into ' ||  interProviderTableName || ' (provider_id,provider_path,name_char,provider_blob,update_date,download_date,import_date,sourcesystem_cd,upload_id)
			    select  provider_id,provider_path, 
                        name_char,provider_blob,
                        update_date,download_date,
                        now(),sourcesystem_cd, ' || uploadId || '
	                     from ' || tempProviderTableName || '  temp ';
					
	--backup the concept_dimension table before creating a new one
	execute 'alter table provider_dimension rename to ' || backupProviderTableName  ||'' ;
    
	-- add index on provider_id, name_char 
    execute 'CREATE INDEX ' || interProviderTableName || '_id_idx ON ' || interProviderTableName  || '(Provider_Id,name_char)';
    execute 'CREATE INDEX ' || interProviderTableName || '_uid_idx ON ' || interProviderTableName  || '(UPLOAD_ID)';

	--backup the concept_dimension table before creating a new one
	execute 'alter table ' || interProviderTableName  || ' rename to provider_dimension' ;
 
EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- %', SQLSTATE, SQLERRM;	
END;

$$;

