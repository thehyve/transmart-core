--
-- Name: i2b2_mrna_index_maint(text, text, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_mrna_index_maint(run_type text DEFAULT 'DROP'::text, tablespace_name text DEFAULT 'INDX'::text, currentjobid bigint DEFAULT NULL::bigint) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

  runType	varchar(100);
  idxExists	bigint;
  pExists	bigint;
  localVar	varchar(20);
  bitmapVar	varchar(20);
  bitmapCompress	varchar(20);
  tableSpace	varchar(50);
   
  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
  

BEGIN

	runType := upper(run_type);
	tableSpace := upper(tablespace_name);
	
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

  PERFORM sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName ;
  procedureName := $$PLSQL_UNIT;

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    cz_start_audit (procedureName, databaseName, jobID);
  END IF;
    	
  stepCt := 0;
  
	--	Determine if de_subject_microarray_data is partitioned, if yes, set localVar to local
  	select count(*)
	into pExists
	from all_tables
	where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
	  and partitioned = 'YES';
	  
	if pExists = 0 then
		localVar := null;
		bitmapVar := null;
		bitmapCompress := 'compress';
	else 
		localVar := 'local';
		bitmapVar := 'bitmap';
		bitmapCompress := null;
	end if;
   
	if runType = 'DROP' then
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Start de_subject_microarray_data index drop',0,stepCt,'Done');
		--	drop the indexes
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX1'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx1');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx1',0,stepCt,'Done');
		end if;
		
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX2'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx2');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx2',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX3'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx3');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx3',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX4'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx4');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx4',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX5'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx5');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx5',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX10'
		  and owner = 'DEAPP';
		
		if idxExists = 1 then
			EXECUTE('drop index deapp.de_microarray_data_idx10');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Drop de_microarray_data_idx10',0,stepCt,'Done');
		end if;
						
	else
		--	add indexes
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Start de_subject_microarray_data index create',0,stepCt,'Done');
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX1'
		  and owner = 'DEAPP';
		  
		if idxExists = 0 then
			EXECUTE('create index deapp.de_microarray_data_idx1 on deapp.de_subject_microarray_data(trial_name, assay_id, probeset_id) ' || localVar || ' nologging compress tablespace "' || tableSpace || '"'); 
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx1',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX2'
		  and owner = 'DEAPP';
		  
		if idxExists = 0 then		
			EXECUTE('create index deapp.de_microarray_data_idx2 on deapp.de_subject_microarray_data(assay_id, probeset_id) ' || localVar || ' nologging compress tablespace "' || tableSpace || '"');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx2',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX3'
		  and owner = 'DEAPP';
		  
		if idxExists = 0 then		
			EXECUTE('create ' || bitmapVar || ' index deapp.de_microarray_data_idx3 on deapp.de_subject_microarray_data(assay_id) ' || localVar || ' nologging ' || bitmapCompress || ' tablespace "' || tableSpace || '"');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx3',0,stepCt,'Done');
		end if;
				
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX4'
		  and owner = 'DEAPP';
		  
		if idxExists = 0 then
			EXECUTE('create ' || bitmapVar || ' index deapp.de_microarray_data_idx4 on deapp.de_subject_microarray_data(probeset_id) ' || localVar || ' nologging ' || bitmapCompress || ' tablespace "' || tableSpace || '"');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx4',0,stepCt,'Done');
		end if;

		if pExists = 0 then
			--	only create this index if the table is not partitioned.  This is the column that the table would be partitioned on
			
			select count(*) 
			into idxExists
			from all_indexes
			where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
			  and index_name = 'DE_MICROARRAY_DATA_IDX5'
			  and owner = 'DEAPP';
			  
			if idxExists = 0 then
				EXECUTE('create index deapp.de_microarray_data_idx5 on deapp.de_subject_microarray_data(trial_source) ' || localVar || ' nologging ' || bitmapCompress || ' tablespace "' || tableSpace || '"');
				stepCt := stepCt + 1;
				cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx5',0,stepCt,'Done');
			end if;
		end if;

/*		not used
	
		select count(*) 
		into idxExists
		from all_indexes
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and index_name = 'DE_MICROARRAY_DATA_IDX10'
		  and owner = 'DEAPP';
		  
		if idxExists = 0 then
			execute immediate('create index deapp.de_microarray_data_idx10 on deapp.de_subject_microarray_data(assay_id, subject_id, probeset_id, zscore) ' || localVar || ' nologging compress tablespace "' || tableSpace || '"');
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Create de_microarray_data_idx10',0,stepCt,'Done');
		end if;
*/
							
	end if;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End FUNCTION'||procedureName,SQL%ROWCOUNT,stepCt,'Done');
	commit;	

	
    ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		cz_end_audit (jobID, 'SUCCESS');
	END IF;

	EXCEPTION
	WHEN OTHERS THEN
		--Handle errors.
		cz_error_handler (jobID, procedureName);
		
		--End Proc
		cz_end_audit (jobID, 'FAIL');
end;
 
 

 
$_$;

