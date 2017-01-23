--
-- Name: insert_encountervisit_fromtemp(character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION insert_encountervisit_fromtemp(temptablename character varying, upload_id numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 

DECLARE
maxEncounterNum NUMERIC; 
BEGIN 

    --Delete duplicate rows with same encounter and patient combination
	-- smuniraju
	-- execute  'DELETE FROM ' || tempTableName || ' t1 WHERE rowid > 
	-- 				   (SELECT  min(rowid) FROM ' || tempTableName || ' t2
	-- 				     WHERE t1.encounter_id = t2.encounter_id 
    --                   AND t1.encounter_id_source = t2.encounter_id_source
    --                   AND nvl(t1.patient_id,'''') = nvl(t2.patient_id,'''')
    --                   AND nvl(t1.patient_id_source,'''') = nvl(t2.patient_id_source,''''))';
	execute  'DELETE FROM ' || tempTableName || ' t1 
			  WHERE ( ctid) NOT IN (
					SELECT  max(ctid) FROM ' || tempTableName || ' t2
					GROUP BY  encounter_id,encounter_id_source)';
	
	 LOCK TABLE  encounter_mapping IN EXCLUSIVE MODE NOWAIT;
    -- select max(encounter_num) into maxEncounterNum from encounter_mapping ;

	--Create new patient(patient_mapping) if temp table patient_ide does not exists 
	-- in patient_mapping table.
	-- smuniraju
    --  execute  ' insert into encounter_mapping (encounter_ide,encounter_ide_source,encounter_num,patient_ide,patient_ide_source,encounter_ide_status, upload_id)
    --  	(select distinctTemp.encounter_id, distinctTemp.encounter_id_source, distinctTemp.encounter_id,  distinctTemp.patient_id,distinctTemp.patient_id_source,''A'',  '|| upload_id ||'
	-- 			from 
	-- 				(select distinct encounter_id, encounter_id_source,patient_id,patient_id_source from ' || tempTableName || '  temp
	-- 				where 
	-- 			     not exists (select encounter_ide from encounter_mapping em where em.encounter_ide = temp.encounter_id and em.encounter_ide_source = temp.encounter_id_source)
	-- 				 and encounter_id_source = ''HIVE'' )   distinctTemp) ' ;

	execute  ' insert into encounter_mapping (encounter_ide,encounter_ide_source,encounter_num,patient_ide,patient_ide_source,encounter_ide_status, upload_id) (
					select distinctTemp.encounter_id, distinctTemp.encounter_id_source, distinctTemp.encounter_id::numeric,  distinctTemp.patient_id,distinctTemp.patient_id_source,''A'',  '|| upload_id ||'
					from (
						select distinct encounter_id, encounter_id_source,patient_id,patient_id_source 
						from ' || tempTableName || ' temp left outer join encounter_mapping em
						on em.encounter_ide = temp.encounter_id and em.encounter_ide_source = temp.encounter_id_source
						where em.encounter_ide_source is null
						and  em.encounter_ide is null
						and encounter_id_source = ''HIVE'' )   distinctTemp) ' ;
	
	-- update patient_num for temp table
	-- smuniraju: Greenplum doesn't support sub query for setting a column value.
	-- update patient_num for temp table
	-- execute  ' UPDATE ' ||  tempTableName || ' SET encounter_num = (SELECT em.encounter_num
	-- 	     FROM encounter_mapping em
	-- 	     WHERE em.encounter_ide = '|| tempTableName ||'.encounter_id
    --       and em.encounter_ide_source = '|| tempTableName ||'.encounter_id_source 
	-- 	     and coalesce(em.patient_ide_source,'''') = coalesce('|| tempTableName ||'.patient_id_source,'''')
	-- 	     and coalesce(em.patient_ide,'''')= coalesce('|| tempTableName ||'.patient_id,''''))
	-- 	     WHERE EXISTS (SELECT em.encounter_num
	-- 	     FROM encounter_mapping em
	-- 	     WHERE em.encounter_ide = '|| tempTableName ||'.encounter_id
    --   	 and em.encounter_ide_source = '||tempTableName||'.encounter_id_source
	-- 	     and coalesce(em.patient_ide_source,'''') = coalesce('|| tempTableName ||'.patient_id_source,'''')
	-- 	     and coalesce(em.patient_ide,'''')= coalesce('|| tempTableName ||'.patient_id,''''))';	
		     
	execute  ' UPDATE ' ||  tempTableName || ' temp SET encounter_num = em.encounter_num
		     FROM encounter_mapping em
		     WHERE em.encounter_ide = '|| tempTableName ||'.encounter_id
             and em.encounter_ide_source = '|| tempTableName ||'.encounter_id_source 
		     and coalesce(em.patient_ide_source,'''') = coalesce(temp.patient_id_source,'''')
		     and coalesce(em.patient_ide,'''')= coalesce(temp.patient_id,'''')
		     and EXISTS (
				SELECT em.encounter_num
				FROM encounter_mapping em
				WHERE em.encounter_ide = '|| tempTableName ||'.encounter_id
				and em.encounter_ide_source = '||tempTableName||'.encounter_id_source
				and coalesce(em.patient_ide_source,'''') = coalesce(temp.patient_id_source,'''')
				and coalesce(em.patient_ide,'''')= coalesce(temp.patient_id,''''))';	

	 execute  'UPDATE visit_dimension vd set 
				inout_cd = temp.inout_cd,
				location_cd = temp.location_cd,
				location_path = temp.location_path,
				start_date = temp.start_date,
				end_date = temp.end_date,
				visit_blob = temp.visit_blob,
				update_date = temp.update_date,
				download_date = temp.download_date,
				import_date = now(),
				sourcesystem_cd = temp.sourcesystem_cd
				from ' || tempTableName || ' temp
				where vd.encounter_num = temp.encounter_num
				and temp.update_date >= vd.update_date';

	-- smuniraju
    -- execute  'insert into visit_dimension  (encounter_num,patient_num,START_DATE,END_DATE,INOUT_CD,LOCATION_CD,VISIT_BLOB,UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,SOURCESYSTEM_CD, UPLOAD_ID)
	--  	select temp.encounter_num, pm.patient_num, temp.START_DATE,temp.END_DATE,temp.INOUT_CD,temp.LOCATION_CD,temp.VISIT_BLOB,
	-- 		temp.update_date, temp.download_date, sysdate, -- import date temp.sourcesystem_cd, '|| upload_id ||'
	-- 		from ' || tempTableName || '  temp , patient_mapping pm 
	-- 		where temp.encounter_num is not null and 
	-- 	      	 not exists (select encounter_num from visit_dimension vd where vd.encounter_num = temp.encounter_num) and 
	-- 			 pm.patient_ide = temp.patient_id and pm.patient_ide_source = temp.patient_id_source
	-- ';
	execute  'insert into visit_dimension (encounter_num,patient_num,START_DATE,END_DATE,INOUT_CD,LOCATION_CD,VISIT_BLOB,UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,SOURCESYSTEM_CD, UPLOAD_ID)
				select temp.encounter_num, pm.patient_num, temp.START_DATE,temp.END_DATE,temp.INOUT_CD,temp.LOCATION_CD,temp.VISIT_BLOB,
				temp.update_date, temp.download_date, current_timestamp, -- import date temp.sourcesystem_cd, '|| upload_id ||'
				from ' || tempTableName || '  temp left outer join patient_mapping pm  
				on	pm.patient_ide = temp.patient_id and pm.patient_ide_source = temp.patient_id_source
				left outer join visit_dimension vd 
				on vd.encounter_num = temp.encounter_num
				where temp.encounter_num is not null 				
				and vd.encounter_num  is null'; 
				
-- smuniraju: Postgres doesn't allow commit within procedures because it is explicity done upon 'END;'	 
-- commit;
		        
EXCEPTION
	WHEN OTHERS THEN
		-- smuniraju: Postgres doesn't allow rollback within procedures because it is explicity when a transaction fails.	 
		-- rollback;
		Raise exception 'An error(-20001) was encountered - % -ERROR- %', SQLSTATE, SQLERRM;	
END;
$$;

