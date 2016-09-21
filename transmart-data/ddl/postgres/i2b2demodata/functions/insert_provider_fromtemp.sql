--
-- Name: insert_provider_fromtemp(character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION insert_provider_fromtemp(tempprovidertablename character varying, upload_id numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$

BEGIN 
	--Delete duplicate rows with same encounter and patient combination
	-- smuniraju: rowid not supported
	-- execute 'DELETE FROM ' || tempProviderTableName || ' t1 WHERE rowid > 
	-- 				   (SELECT  min(rowid) FROM ' || tempProviderTableName || ' t2
	-- 				     WHERE t1.provider_id = t2.provider_id 
    --                                         AND t1.provider_path = t2.provider_path
    --                                         )';
	execute 'DELETE FROM ' || tempProviderTableName || ' t1 
			 WHERE ( ctid) NOT IN ( 
				SELECT  max(ctid)
				FROM ' || tempProviderTableName || ' t2
				GROUP BY  provider_path)';
	
 execute 'UPDATE patient_dimension set 
			provider_id = temp.provider_id,
			name_char = temp.name_char,
			provider_blob = provider_blob,
			IMPORT_DATE=now(),
			UPDATE_DATE=temp.UPDATE_DATE,
			DOWNLOAD_DATE=temp.DOWNLOAD_DATE,
			SOURCESYSTEM_CD=temp.SOURCESYSTEM_CD,
			UPLOAD_ID = '||  upload_id || '
			from provider_dimension pd 
			inner join ' || tempProviderTableName || ' temp
			on  pd.provider_path = temp.provider_path
			where temp.update_date >= pd.update_date) ';

   
    --Create new patient(patient_mapping) if temp table patient_ide does not exists 
	-- in patient_mapping table.
	-- smuniraju: not exists => co-related query.
	-- execute 'insert into provider_dimension  (provider_id,provider_path,name_char,provider_blob,update_date,download_date,import_date,sourcesystem_cd,upload_id)
	-- 		    select  provider_id,provider_path, 
    --                     name_char,provider_blob,
    --                     update_date,download_date,
    --                     now(),sourcesystem_cd, ' || upload_id || '	                    
    --                      from ' || tempProviderTableName || '  temp
	-- 				where not exists (select provider_id from provider_dimension pd where pd.provider_path = temp.provider_path )';
	execute 'insert into provider_dimension  (provider_id,provider_path,name_char,provider_blob,update_date,download_date,import_date,sourcesystem_cd,upload_id)
			    select  provider_id,provider_path, 
                        name_char,provider_blob,
                        update_date,download_date,
                        now(),sourcesystem_cd, ' || upload_id || '	                    
                        from ' || tempProviderTableName || '  temp left outer join provider_dimension pd 
						on pd.provider_path = temp.provider_path 
						where pd.provider_path is null';   
EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- %', SQLSTATE, SQLERRM;	
END;

$$;

