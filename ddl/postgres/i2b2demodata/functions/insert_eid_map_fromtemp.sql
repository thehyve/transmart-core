--
-- Name: insert_eid_map_fromtemp(character varying, numeric); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION insert_eid_map_fromtemp(tempeidtablename character varying, upload_id numeric, OUT errormsg character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$

DECLARE
 existingEncounterNum VARCHAR(32);
 maxEncounterNum NUMERIC;
 -- TYPE distinctEIdCurTyp IS CURSOR;
 -- distinctEidCur   distinctEIdCurTyp;
 distinctEidCur CURSOR;
 sql_stmt  VARCHAR(400); 
 disEncounterId VARCHAR(100); 
 disEncounterIdSource VARCHAR(100);

BEGIN
 sql_stmt := ' SELECT distinct encounter_id,encounter_id_source from ' || tempEidTableName ||' ';

-- smuniraju: rowid not supported in postgres/greenplum, instead using ctid (and gp_segment_id in greenplum)
-- execute ' delete  from ' || tempEidTableName ||  ' t1  where 
-- rowid > (select min(rowid) from ' || tempEidTableName || ' t2 
-- where t1.encounter_map_id = t2.encounter_map_id
-- and t1.encounter_map_id_source = t2.encounter_map_id_source
-- and t1.encounter_id = t2.encounter_id
-- and t1.encounter_id_source = t2.encounter_id_source) ';

 execute 'delete  from ' || tempEidTableName ||  ' t1  
		  where ( ctid) NOT IN (select  max(ctid) from ' || tempEidTableName || ' t2 
		  group by  encounter_map_id,encounter_map_id_source,encounter_id,encounter_id_source )';


 LOCK TABLE  encounter_mapping IN EXCLUSIVE MODE NOWAIT;
 select max(encounter_num) into maxEncounterNum from encounter_mapping ; 
 
if maxEncounterNum is null then 
  maxEncounterNum := 0;
end if;

  open distinctEidCur for EXECUTE(sql_stmt) ;
 
   loop
     FETCH distinctEidCur INTO disEncounterId, disEncounterIdSource;
      -- smuniraju: %NOTFOUND not supported in postgres.
	  -- EXIT WHEN distinctEidCur%NOTFOUND;
	 EXIT WHEN NOT FOUND;
      -- dbms_output.put_line(disEncounterId);
        
  if  disEncounterIdSource = 'HIVE'  THEN    
   begin
    --check if hive NUMERIC exist, if so assign that NUMERIC to reset of map_id's within that pid
    select encounter_num into existingEncounterNum from encounter_mapping where encounter_num = disEncounterId and encounter_ide_source = 'HIVE';
    
    EXCEPTION  
       when NO_DATA_FOUND THEN
           existingEncounterNum := null;
    end;
   
   if existingEncounterNum is not null then 
		-- smuniraju: NOT EXISTS clause reults in a co-related queries which are not supported in greenplum
        -- execute ' update ' || tempEidTableName ||' set encounter_num = encounter_id, process_status_flag = ''P''
        -- where encounter_id = ' || disEncounterId || ' and not exists (select 1 from encounter_mapping em where em.encounter_ide = encounter_map_id
        -- and em.encounter_ide_source = encounter_map_id_source)';		
		execute ' 	update ' || tempEidTableName ||' set 
					encounter_num = encounter_id::numeric, process_status_flag = ''P''
					from encounter_mapping em 
					where em.encounter_ide = encounter_map_id
					and em.encounter_ide_source = encounter_map_id_source
					and encounter_id = ' || disEncounterId || ' 
					and em.encounter_ide is null
					and em.encounter_ide_source is null ';
   else 
        -- generate new patient_num i.e. take max(_num) + 1 
        if maxEncounterNum < disEncounterId then 
            maxEncounterNum := disEncounterId;
        end if ;
		-- smuniraju : NOT EXISTS clause reults in a co-related queries which are not supported in greenplum
        -- execute ' update ' || tempEidTableName ||' set encounter_num = encounter_id, process_status_flag = ''P'' where 
        -- encounter_id = ' || disEncounterId || ' and encounter_id_source = ''HIVE'' and not exists (select 1 from encounter_mapping em where em.encounter_ide = encounter_map_id
        -- and em.encounter_ide_source = encounter_map_id_source)' ;		
		execute '   update ' || tempEidTableName ||' set 
					encounter_num = encounter_id::numeric, process_status_flag = ''P''
					from encounter_mapping em 
					where em.encounter_ide = encounter_map_id
					and em.encounter_ide_source = encounter_map_id_source
					and encounter_id = ' || disEncounterId || ' 
					and encounter_id_source = ''HIVE'' 
					and em.encounter_ide is null
					and em.encounter_ide_source is null ';      
   end if;    
   
   -- test if record fectched
   -- dbms_output.put_line(' HIVE ');

 else 
    begin
       select encounter_num into existingEncounterNum from encounter_mapping where encounter_ide = disEncounterId and 
        encounter_ide_source = disEncounterIdSource ; 

       -- test if record fetched. 
       EXCEPTION
           WHEN NO_DATA_FOUND THEN
           existingEncounterNum := null;
       end;
       if existingEncounterNum is not  null then 
			-- smuniraju: NOT EXISTS clause reults in a co-related queries which are not supported in greenplum
            -- execute ' update ' || tempEidTableName ||' set encounter_num = ' || existingEncounterNum || ' , process_status_flag = ''P''
            -- where encounter_id = ' || disEncounterId || ' and not exists (select 1 from encounter_mapping em where em.encounter_ide = encounter_map_id
			-- and em.encounter_ide_source = encounter_map_id_source)' ;
		execute ' update ' || tempEidTableName ||' set 
				  encounter_num = ' || existingEncounterNum || '::numeric , process_status_flag = ''P''
				  from encounter_mapping em 
				  where em.encounter_ide = encounter_map_id and em.encounter_ide_source = encounter_map_id_source
				  and encounter_id = ' || disEncounterId || ' 
				  and em.encounter_ide is null 
				  and em.encounter_ide_source is null' ;
       else 
            maxEncounterNum := maxEncounterNum + 1 ;
			
			--TODO : add update colunn
             execute ' insert into ' || tempEidTableName ||' (encounter_map_id,encounter_map_id_source,encounter_id,encounter_id_source,encounter_num,process_status_flag
             ,encounter_map_id_status,update_date,download_date,import_date,sourcesystem_cd) 
             values(' || maxEncounterNum || ',''HIVE'',' || maxEncounterNum || ',''HIVE'',' || maxEncounterNum ||',''P'',''A'',current_timestamp,current_timestamp,current_timestamp,''edu.harvard.i2b2.crc'')'; 
             
            -- smuniraju: NOT EXISTS clause reults in a co-related queries which are not supported in greenplum
	        -- execute ' update ' || tempEidTableName ||' set encounter_num =  ' || maxEncounterNum || ' , process_status_flag = ''P'' 
            -- where encounter_id = ' || disEncounterId || ' and  not exists (select 1 from 
            -- encounter_mapping em where em.encounter_ide = encounter_map_id
            -- and em.encounter_ide_source = encounter_map_id_source)' ;
			execute ' update ' || tempEidTableName ||' set 
					  encounter_num = ' || maxEncounterNum || ' , process_status_flag = ''P''
					  from encounter_mapping em 
					  where em.encounter_ide = encounter_map_id and em.encounter_ide_source = encounter_map_id_source
					  and encounter_id = ' || disEncounterId || ' 
					  and em.encounter_ide is null 
					  and em.encounter_ide_source is null' ;
            
       end if ;
 end if; 

END LOOP;
close distinctEidCur ;

-- smuniraju Postgres doesn't allow commit within procedures because it is explicity done upon 'END;'
-- commit;

 -- do the mapping update if the update date is old
   execute 'update encounter_mapping em set 
			encounter_num = temp.encounter_id::numeric,
			patient_ide = temp.patient_map_id ,
			patient_ide_source  = temp.patient_map_id_source ,
			encounter_ide_status = temp.encounter_map_id_status  ,
			update_date = temp.update_date,
			download_date = temp.download_date ,
			import_date = now() ,
			sourcesystem_cd  = temp.sourcesystem_cd ,
			upload_id = ' || upload_id || ' 
			from ' || tempEidTableName || ' temp
            where em.encounter_ide = temp.encounter_map_id 
			and em.encounter_ide_source = temp.encounter_map_id_source 
			and temp.encounter_id_source = ''HIVE'' and temp.process_status_flag is null  
			and coalesce(em.update_date,to_date(''01-JAN-1900'',''DD-MON-YYYY''))<= coalesce(temp.update_date,to_date(''01-JAN-1900'',''DD-MON-YYYY'')) ' ;

-- insert new mapping records i.e flagged P

execute ' insert into encounter_mapping (encounter_ide,encounter_ide_source,encounter_ide_status,encounter_num,patient_ide,patient_ide_source,update_date,download_date,import_date,sourcesystem_cd,upload_id) 
			select 	encounter_map_id,encounter_map_id_source,encounter_map_id_status,encounter_num,patient_map_id,patient_map_id_source,update_date,download_date,current_timestamp,sourcesystem_cd,' || upload_id || ' 
			from ' || tempEidTableName || '  
			where process_status_flag = ''P'' ' ; 

-- smuniraju Postgres doesn't allow commit within procedures because it is explicity done upon 'END;'
-- commit;
EXCEPTION
   WHEN OTHERS THEN
	  -- smuniraju
      /*if distinctEidCur%isopen then
          close distinctEidCur;
      end if;*/
	  RAISE EXCEPTION 'An error was encountered - % -ERROR- %', SQLSTATE, SQLERRM;
	  
	  begin
		close distinctEidCur;
		EXCEPTION
			WHEN OTHERS THEN
			RAISE NOTICE 'Error occurred when attempting to close cursor.';
	  end;	
      -- smuniraju Postgres doesn't allow rollback within procedures because it is explicity when a transaction fails.
      -- rollback;
      
end;
$$;

