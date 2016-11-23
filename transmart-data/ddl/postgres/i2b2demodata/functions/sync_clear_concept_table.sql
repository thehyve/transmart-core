--
-- Name: sync_clear_concept_table(character varying, character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION sync_clear_concept_table(tempconcepttablename character varying, backupconcepttablename character varying, uploadid numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ DECLARE

DECLARE
interConceptTableName  varchar(400);

BEGIN 
	interConceptTableName := backupConceptTableName || '_inter';
	
		--Delete duplicate rows with same encounter and patient combination
		-- smuniraju
	-- execute 'DELETE FROM ' || tempConceptTableName || ' t1 WHERE rowid > 
	-- 				   (SELECT  min(rowid) FROM ' || tempConceptTableName || ' t2
	-- 				     WHERE t1.concept_cd = t2.concept_cd 
    --                                         AND t1.concept_path = t2.concept_path
    --                                         )';
	execute 'DELETE FROM ' || tempConceptTableName || ' t1 
	         WHERE ( ctid) NOT IN  
					   (SELECT   max(rowid) FROM ' || tempConceptTableName || ' t2
					     GROUP BY  concept_path,concept_cd)';
						 
    execute 'create table ' ||  interConceptTableName || ' (
    CONCEPT_CD          VARCHAR(50) NOT NULL,
	CONCEPT_PATH    	VARCHAR(700) NOT NULL,
	NAME_CHAR       	VARCHAR(2000) NULL,
	CONCEPT_BLOB        TEXT NULL,
	UPDATE_DATE         DATE NULL,
	DOWNLOAD_DATE       DATE NULL,
	IMPORT_DATE         DATE NULL,
	SOURCESYSTEM_CD     VARCHAR(50) NULL,
	UPLOAD_ID       	NUMERIC(38,0) NULL,
    CONSTRAINT '|| interConceptTableName ||'_pk  PRIMARY KEY(CONCEPT_PATH)
	 )';
    
    --Create new patient(patient_mapping) if temp table patient_ide does not exists 
	-- in patient_mapping table.
	execute 'insert into '|| interConceptTableName ||'  (concept_cd,concept_path,name_char,concept_blob,update_date,download_date,import_date,sourcesystem_cd,upload_id)
			    select  concept_cd, substr(concept_path,1,700),
                        name_char,concept_blob,
                        update_date,download_date,
                        current_timestamp,sourcesystem_cd,
                         ' || uploadId || '  from ' || tempConceptTableName || '  temp ';
	--backup the concept_dimension table before creating a new one
	execute 'alter table concept_dimension rename to ' || backupConceptTableName  ||'' ;
    
	-- add index on upload_id 
    execute 'CREATE INDEX ' || interConceptTableName || '_uid_idx ON ' || interConceptTableName || '(UPLOAD_ID)';

    -- add index on upload_id 
    execute 'CREATE INDEX ' || interConceptTableName || '_cd_idx ON ' || interConceptTableName || '(concept_cd)';

    
    --backup the concept_dimension table before creating a new one
	execute 'alter table ' || interConceptTableName  || ' rename to concept_dimension' ;
 
EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- %', SQLSTATE, SQLERRM;	
END;
$$;

