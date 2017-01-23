  CREATE OR REPLACE PROCEDURE TM_CZ.PARTITION_GWAS_DATA
AS

  sqlText		varchar2(1000);
  tText			varchar2(1000);
  pExists		number;
  
    --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);

BEGIN

  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := -1;

  SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
  procedureName := $$PLSQL_UNIT;

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(jobID IS NULL or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    cz_start_audit (procedureName, databaseName, jobID);
  END IF;
    
	for gwas_data in (select distinct bio_assay_analysis_id from biomart.bio_assay_analysis_gwas )
	loop

		stepCt := 0;
		stepCt := stepCt + 1;
		tText := 'Starting load for ' || to_char(gwas_data.bio_assay_analysis_id);
		cz_write_audit(jobId,databaseName,procedureName,tText,0,stepCt,'Done');
		
		--	Create partition in gwas_partition if it doesn't exist else truncate partition
		
		select count(*)
			into pExists
			from all_tab_partitions
			where table_name = 'GWAS_PARTITION'
			  and partition_name = to_char(gwas_data.bio_assay_analysis_id);
		
		if pExists = 0 then				
			--	needed to add partition to GWAS_PARTITION

			sqlText := 'alter table biomart.GWAS_PARTITION add PARTITION "' || to_char(gwas_data.bio_assay_analysis_id) || '"  VALUES (' || 
					   to_char(gwas_data.bio_assay_analysis_id) || ') ' ||
						   'NOLOGGING TABLESPACE "BIOMART" ';
			execute immediate(sqlText);
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Adding partition to GWAS_PARTITION',0,stepCt,'Done');
			
		else
			sqlText := 'alter table biomart.GWAS_PARTITION truncate partition "' || to_char(gwas_data.bio_assay_analysis_id) || '"';
			execute immediate(sqlText);
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Truncating partition in GWAS_PARTITION',0,stepCt,'Done');
		end if;

		insert into biomart.GWAS_PARTITION
		(bio_asy_analysis_gwas_id
		,bio_assay_analysis_id
		,rs_id
		,p_value_char
		,p_value
		,log_p_value
		,etl_id
		,ext_data
		)
		select g.bio_asy_analysis_gwas_id
			  ,g.bio_assay_analysis_id
			  ,g.rs_id
			  ,g.p_value_char
			  ,g.p_value
			  ,g.log_p_value
			  ,g.etl_id
			  ,g.ext_data
		from biomart.bio_assay_analysis_gwas g
		where g.bio_assay_analysis_id = gwas_data.bio_assay_analysis_id; 
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis into GWAS_PARTITION',SQL%ROWCOUNT,stepCt,'Done');	
		commit;
		
	end loop;
	
	   ---Cleanup OVERALL JOB if this proc is being run standalone
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,procedureName,0,stepCt,'Done');

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
		--select 16 into rtn_code from dual;
END;