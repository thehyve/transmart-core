--
-- Name: sync_clear_modifier_table(character varying, character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION sync_clear_modifier_table(tempmodifiertablename character varying, backupmodifiertablename character varying, uploadid numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE


interModifierTableName  varchar(400);


BEGIN
	interModifierTableName := backupModifierTableName || '_inter';

	--Delete duplicate rows with same modifier_path and modifier cd
	EXECUTE 'DELETE FROM ' || tempModifierTableName || ' t1 WHERE oid >
					   (SELECT  min(oid) FROM ' || tempModifierTableName || ' t2
					     WHERE t1.modifier_cd = t2.modifier_cd
                                            AND t1.modifier_path = t2.modifier_path
                                            )';

    EXECUTE 'create table ' ||  interModifierTableName || ' (
        MODIFIER_CD          varchar(50) NOT NULL,
	MODIFIER_PATH    	varchar(700) NOT NULL,
	NAME_CHAR       	varchar(2000) NULL,
	MODIFIER_BLOB        text NULL,
	UPDATE_DATE         timestamp NULL,
	DOWNLOAD_DATE       timestamp NULL,
	IMPORT_DATE         timestamp NULL,
	SOURCESYSTEM_CD     varchar(50) NULL,
	UPLOAD_ID       	numeric(38,0) NULL,
    CONSTRAINT '|| interModifierTableName ||'_pk  PRIMARY KEY(MODIFIER_PATH)
	 )';

    --Create new patient(patient_mapping) if temp table patient_ide does not exists
	-- in patient_mapping table.
	EXECUTE 'insert into '|| interModifierTableName ||'  (modifier_cd,modifier_path,name_char,modifier_blob,update_date,download_date,import_date,sourcesystem_cd,upload_id)
			    PERFORM  modifier_cd, substring(modifier_path from 1 for 700),
                        name_char,modifier_blob,
                        update_date,download_date,
                        LOCALTIMESTAMP,sourcesystem_cd,
                         ' || uploadId || '  from ' || tempModifierTableName || '  temp ';
	--backup the modifier_dimension table before creating a new one
	EXECUTE 'alter table modifier_dimension rename to ' || backupModifierTableName  ||'' ;

	-- add index on upload_id
    EXECUTE 'CREATE INDEX ' || interModifierTableName || '_uid_idx ON ' || interModifierTableName || '(UPLOAD_ID)';

    -- add index on upload_id
    EXECUTE 'CREATE INDEX ' || interModifierTableName || '_cd_idx ON ' || interModifierTableName || '(modifier_cd)';

    --backup the modifier_dimension table before creating a new one
	EXECUTE 'alter table ' || interModifierTableName  || ' rename to modifier_dimension' ;

EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- %',SQLSTATE,SQLERRM;
END;
 
$$;

