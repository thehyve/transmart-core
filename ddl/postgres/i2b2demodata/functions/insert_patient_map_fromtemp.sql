--
-- Name: insert_patient_map_fromtemp(character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION insert_patient_map_fromtemp(temppatienttablename character varying, upload_id numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

BEGIN 
	
	-- Create new patient mapping entry for HIVE patient's if they are not already mapped in mapping table
	-- smuniraju: not exists => co-related
	-- execute 'insert into patient_mapping (
	-- 		select distinct temp.patient_id, temp.patient_id_source,''A'',temp.patient_id ,' || upload_id || '
	-- 		from ' || tempPatientTableName ||'  temp 
	-- 		where temp.patient_id_source = ''HIVE'' and 
   	-- 		not exists (
	--			select patient_ide from patient_mapping pm where pm.patient_num = temp.patient_id and pm.patient_ide_source = temp.patient_id_source) 
	-- 	)'; 
    
	execute 'insert into patient_mapping (patient_ide, patient_ide_source, patient_ide_status, patient_num, upload_id) (
				select distinct temp.patient_id, temp.patient_id_source,''A'',temp.patient_id::numeric ,' || upload_id || '
				from ' || tempPatientTableName ||'  temp left outer join patient_mapping pm
				on pm.patient_num = temp.patient_id and pm.patient_ide_source = temp.patient_id_source			
				where temp.patient_id_source = ''HIVE'' 
				and pm.patient_num is null 
				and pm.patient_ide_source is null)'; 
		
    --Create new visit for above inserted encounter's
	--If Visit table's encounter and patient num does match temp table,
	--then new visit information is created.
	execute 'UPDATE patient_dimension pd set 
			 VITAL_STATUS_CD= temp.VITAL_STATUS_CD,
			 BIRTH_DATE= temp.BIRTH_DATE,
			 DEATH_DATE= temp.DEATH_DATE,
			 SEX_CD= temp.SEX_CD,
			 AGE_IN_YEARS_NUM=temp.AGE_IN_YEARS_NUM,
			 LANGUAGE_CD=temp.LANGUAGE_CD,
			 RACE_CD=temp.RACE_CD,
			 MARITAL_STATUS_CD=temp.MARITAL_STATUS_CD,
			 RELIGION_CD=temp.RELIGION_CD,
			 ZIP_CD=temp.ZIP_CD,
			 STATECITYZIP_PATH =temp.STATECITYZIP_PATH,
			 PATIENT_BLOB=temp.PATIENT_BLOB,
			 UPDATE_DATE=temp.UPDATE_DATE,
			 DOWNLOAD_DATE=temp.DOWNLOAD_DATE,
			 SOURCESYSTEM_CD=temp.SOURCESYSTEM_CD,
			 UPLOAD_ID = '|| upload_id || '
			 from ' || tempPatientTableName || ' temp
			 where pd.patient_num = temp.patient_num
			 and temp.update_date > pd.update_date';
EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- %', SQLSTATE, SQLERRM;	
END;

$$;

