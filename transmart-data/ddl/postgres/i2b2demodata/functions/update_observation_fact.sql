--
-- Name: update_observation_fact(character varying, numeric, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION update_observation_fact(upload_temptable_name character varying, upload_id numeric, appendflag numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN



--Delete duplicate records(encounter_ide,patient_ide,concept_cd,start_date,modifier_cd,provider_id)
-- smuniraju: rowid not implemented in postgres
-- execute 'DELETE FROM ' || upload_temptable_name ||'  t1 
--  where rowid > (select min(rowid) from ' || upload_temptable_name ||' t2 
--    where t1.encounter_id = t2.encounter_id  
--          and
--          t1.encounter_id_source = t2.encounter_id_source
--          and
--          t1.patient_id = t2.patient_id 
--          and 
--          t1.patient_id_source = t2.patient_id_source
--          and 
--          t1.concept_cd = t2.concept_cd                
--          and 
--          t1.start_date = t2.start_date
--          and 
--          nvl(t1.modifier_cd,''xyz'') = nvl(t2.modifier_cd,''xyz'')
--		  and 
--		  t1.instance_num = t2.instance_num
--          and 
--          t1.provider_id = t2.provider_id)';
execute 'DELETE FROM ' || upload_temptable_name ||'  
		 WHERE ( ctid) not in (
			SELECT  max(ctid) FROM ' || upload_temptable_name ||' 
			GROUP BY  encounter_id,encounter_id_source,patient_id,patient_id_source, concept_cd,start_date,modifier_cd,provider_id,instance_num ORDER BY encounter_id,encounter_id_source)';
          
--Delete records having null in start_date
execute 'DELETE FROM ' || upload_temptable_name ||'  t1           
 where t1.start_date is null';
           
           
--One time lookup on encounter_ide to get encounter_num 
-- smuniraju: Greenplum doesn't support sub query for assigning values.
-- execute 'UPDATE ' ||  upload_temptable_name
--  || ' SET encounter_num = (SELECT em.encounter_num
-- 		     FROM encounter_mapping em
-- 		     WHERE em.encounter_ide = ' || upload_temptable_name||'.encounter_id
--                   and em.encounter_ide_source = '|| upload_temptable_name||'.encounter_id_source) 
--                   WHERE EXISTS (SELECT em.encounter_num
-- 		     FROM encounter_mapping em
-- 		     WHERE em.encounter_ide = '|| upload_temptable_name||'.encounter_id
--		     and em.encounter_ide_source = '||upload_temptable_name||'.encounter_id_source)';		     

execute 'UPDATE ' ||  upload_temptable_name || ' SET encounter_num = em.encounter_num
	FROM encounter_mapping em
	WHERE em.encounter_ide = ' || upload_temptable_name||'.encounter_id
        and em.encounter_ide_source = '|| upload_temptable_name||'.encounter_id_source) 
        and EXISTS (
		SELECT em.encounter_num
		FROM encounter_mapping em
		WHERE em.encounter_ide = '|| upload_temptable_name||'.encounter_id
                and em.encounter_ide_source = '||upload_temptable_name||'.encounter_id_source)';	     

--One time lookup on patient_ide to get patient_num 
-- smuniraju: Greenplum doesn't support sub query for assigning values.
-- execute 'UPDATE ' ||  upload_temptable_name
--  || ' SET patient_num = (SELECT pm.patient_num
-- 		     FROM patient_mapping pm
-- 		     WHERE pm.patient_ide = '|| upload_temptable_name||'.patient_id
--                      and pm.patient_ide_source = '|| upload_temptable_name||'.patient_id_source
-- 	 	    )WHERE EXISTS (SELECT pm.patient_num 
-- 		     FROM patient_mapping pm
-- 		     WHERE pm.patient_ide = '|| upload_temptable_name||'.patient_id
--                      and pm.patient_ide_source = '||upload_temptable_name||'.patient_id_source)';		     

execute 'UPDATE ' ||  upload_temptable_name || ' SET patient_num = pm.patient_num
	FROM patient_mapping pm
	WHERE pm.patient_ide = '|| upload_temptable_name||'.patient_id
        and pm.patient_ide_source = '|| upload_temptable_name||'.patient_id_source
	and EXISTS (
		SELECT pm.patient_num 
		FROM patient_mapping pm
		WHERE pm.patient_ide = '|| upload_temptable_name||'.patient_id
                and pm.patient_ide_source = '||upload_temptable_name||'.patient_id_source)';		     

IF (appendFlag = 0) THEN
--Archive records which are to be deleted in observation_fact table
execute 'INSERT ALL INTO  archive_observation_fact 
		SELECT obsfact.*, ' || upload_id ||' archive_upload_id 
		FROM observation_fact obsfact
		WHERE obsfact.encounter_num IN 
			(SELECT temp_obsfact.encounter_num
			FROM  ' ||upload_temptable_name ||' temp_obsfact
                        group by temp_obsfact.encounter_num  
            )';


--Delete above archived row from observation_fact
execute 'DELETE  observation_fact 
		 WHERE EXISTS (
				SELECT archive.encounter_num
				FROM archive_observation_fact  archive
				where archive.archive_upload_id = '||upload_id ||'
                AND archive.encounter_num=observation_fact.encounter_num
				AND archive.concept_cd = observation_fact.concept_cd
				AND archive.start_date = observation_fact.start_date
         )';
END IF;

-- if the append is true, then do the update else do insert all
IF (appendFlag = 0) THEN

--Transfer all rows from temp_obsfact to observation_fact
execute 'INSERT ALL INTO observation_fact(encounter_num,concept_cd, patient_num,provider_id, start_date,modifier_cd,instance_num,valtype_cd,tval_char,nval_num,valueflag_cd,
quantity_num,confidence_num,observation_blob,units_cd,end_date,location_cd, update_date,download_date,import_date,sourcesystem_cd,
upload_id) 
SELECT encounter_num,concept_cd, patient_num,provider_id, start_date,modifier_cd,instance_num,valtype_cd,tval_char,nval_num,valueflag_cd,
quantity_num,confidence_num,observation_blob,units_cd,end_date,location_cd, update_date,download_date,current_timestamp import_date,sourcesystem_cd,
temp.upload_id 
FROM ' || upload_temptable_name ||' temp
where temp.patient_num is not null and  temp.encounter_num is not null';
ELSE				
	execute ' UPDATE observation_fact  set 
			valtype_cd = temp.valtype_cd,
            tval_char = temp.tval_char,
			nval_num = temp.nval_num ,
			valueflag_cd = temp.valueflag_cd,
			quantity_num = temp.quantity_num,
			confidence_num = temp.confidence_num ,
			observation_blob = temp.observation_blob,
			units_cd = temp.units_cd,
			end_date = temp.end_date,
			location_cd = temp.location_cd,
			update_date= temp.update_date,
			download_date = temp.download_date,
			import_date = now(),
			sourcesystem_cd = temp.sourcesystem_cd,
			UPLOAD_ID = ' || upload_id || '
			from observation_fact obsfact 
			inner join ' || upload_temptable_name || ' temp
			on  obsfact.encounter_num = temp.encounter_num 
			and obsfact.patient_num = temp.patient_num
			and obsfact.concept_cd = temp.concept_cd
			and obsfact.start_date = temp.start_date
			and obsfact.provider_id = temp.provider_id
			and obsfact.modifier_cd = temp.modifier_cd
			and obsfact.instance_num = temp.instance_num
			where coalesce(observation_fact.update_date,to_date(''1900-01-01'',''YYYY-MM-DD''))<= coalesce(temp.update_date,to_date(''1900-01-01'',''YYYY-MM-DD'')) ';
END IF;

EXCEPTION
	WHEN OTHERS THEN
		RAISE EXCEPTION 'An error was encountered - % -ERROR- % ', SQLSTATE, SQLERRM;	
END;
$$;


SET default_with_oids = false;

