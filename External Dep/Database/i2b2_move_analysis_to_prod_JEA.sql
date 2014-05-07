CREATE OR REPLACE PROCEDURE TM_CZ."I2B2_MOVE_ANALYSIS_TO_PROD_NEW" 
(i_etl_id        number    := -1
,i_job_id        number    := null
)
AS
    -- create indexes using parallele 8  -zhanh101 5/10/2013 use ~20-30% original time  
    --Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);
    
    v_etl_id					number(18,0);
    v_bio_assay_analysis_id		number(18,0);
    v_data_type					varchar2(50);
    v_sqlText					varchar2(2000);
    v_exists                    int;
    v_GWAS_staged				int;
    v_EQTL_staged				int;
	v_gwas_indx					int;
	v_eqtl_indx					int;
	v_max_ext_flds				int;
    
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
        
    stepCt := 1;    
    cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_move_analysis_to_prod',0,stepCt,'Done');
    
	v_GWAS_staged := 0;
	v_EQTL_staged := 0;
	v_gwas_indx := 0;
	
    --    load staged analysis to array
    
	for stage_rec in 
		(select baa.bio_assay_analysis_id
			  ,lz.etl_id
			  ,lz.study_id
			  ,case when lz.data_type = 'Metabolic GWAS' then 'GWAS' else lz.data_type end as data_type
			  ,lz.data_type as orig_data_type
			  ,lz.analysis_name
		from tm_lz.lz_src_analysis_metadata lz
			,biomart.bio_assay_analysis baa
		where lz.status = 'STAGED'
		  and lz.study_id = baa.etl_id
		  and lz.etl_id = baa.etl_id_source
		  and case when i_etl_id = -1 then 1
               when lz.etl_id = i_etl_id then 1
               else 0 end = 1)
	loop
		stepCt := stepCt + 1;
        cz_write_audit(jobId,databaseName,procedureName,'Loading ' || stage_rec.study_id || ' ' || stage_rec.orig_data_type || ' ' ||
                       stage_rec.analysis_name,0,stepCt,'Done');
           		
		v_bio_assay_analysis_id := stage_rec.bio_assay_analysis_id;
		v_etl_id := stage_rec.etl_id;
		v_data_type := stage_rec.data_type;
		
		--	get max nbr fields in ext_data for original data type
			
		select max(field_idx) into v_max_ext_flds
		from biomart.bio_asy_analysis_data_idx
		where ext_type = stage_rec.orig_data_type;
		
        if stage_rec.data_type = 'GWAS' then
			--	GWAS data
            v_GWAS_staged := 1;
    
			if v_gwas_indx = 0 then
				--	disable indexes if loading GWAS data
				for gwas_idx in (select index_name
									   ,table_name
								 from all_indexes 
					    		 where owner = 'BIOMART' 
								   and table_name in ('BIO_ASSAY_ANALYSIS_GWAS','BIO_ASY_ANALYSIS_GWAS_TOP50') 
								   and partitioned = 'NO' )
				loop
					v_sqlText := 'alter index ' || gwas_idx.index_name || ' unusable';
					stepCt := stepCt + 1;
					cz_write_audit(jobId,databaseName,procedureName,'Disabling index ' || gwas_idx.index_name || ' on ' || gwas_idx.table_name,SQL%ROWCOUNT,stepCt,'Done');
					execute immediate(v_sqlText);
					stepCt := stepCt + 1;
					cz_write_audit(jobId,databaseName,procedureName,'Disabling complete',SQL%ROWCOUNT,stepCt,'Done');       
				end loop;
				v_gwas_indx := 1;
			end if;
			
			--	check if partition exists for bio_assay_analysis_id, if not, add, if yes, truncate
			select count(*) into v_exists
			from all_tab_partitions
			where table_name = 'BIO_ASSAY_ANALYSIS_GWAS'
			  and partition_name = to_char(v_bio_assay_analysis_id);
		
			if v_exists = 0 then	
				--	add partition to bio_assay_analysis_gwas
				v_sqlText := 'alter table biomart.bio_assay_analysis_gwas add PARTITION "' || to_char(v_bio_assay_analysis_id) || '"  VALUES (' || 
						    to_char(v_bio_assay_analysis_id) || ') ' ||
						   'NOLOGGING TABLESPACE "BIOMART" ';
				execute immediate(v_sqlText);
				stepCt := stepCt + 1;
				cz_write_audit(jobId,databaseName,procedureName,'Adding partition to bio_assay_analysis_gwas',0,stepCt,'Done');
			else
				--	truncate existing partition
				v_sqlText := 'alter table biomart.bio_assay_analysis_gwas truncate partition "' || to_char(v_bio_assay_analysis_id) || '"';
				execute immediate(v_sqlText);
				stepCt := stepCt + 1;
				cz_write_audit(jobId,databaseName,procedureName,'Truncating partition in bio_assay_analysis_gwas',0,stepCt,'Done');
			end if;
			
            insert into biomart.bio_assay_analysis_gwas
            (bio_asy_analysis_gwas_id
            ,bio_assay_analysis_id
            ,rs_id
            ,p_value
            ,p_value_char
            ,etl_id
            ,ext_data
            ,log_p_value)
            select bio_asy_analysis_gwas_id
                  ,bio_assay_analysis_id
                  ,rs_id
                  ,to_binary_double(p_value_char)
                  ,p_value_char
                  ,etl_id
                  ,case when length(ext_data)-length(replace(ext_data,';','')) < v_max_ext_flds
						then tm_cz.repeat_char(ext_data,v_max_ext_flds-(length(ext_data)-length(replace(ext_data,';',''))),';')
						else ext_data
				   end
                  ,log(10,to_binary_double(p_value_char))*-1
            from biomart_stage.bio_assay_analysis_gwas
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from biomart.bio_assay_analysis_gwas',SQL%ROWCOUNT,stepCt,'Done');
            commit; 
			
			--	update data_count in bio_assay_analysis
			
            update biomart.bio_assay_analysis baa
			set data_count=(select count(*) from biomart.bio_assay_analysis_gwas x
							where x.bio_assay_analysis_id=v_bio_assay_analysis_id) 
            where baa.bio_assay_analysis_id=v_bio_assay_analysis_id;
            stepCt := stepCt +1;
            cz_write_audit(jobId,databaseName,procedureName,'Update data_count for analysis',SQL%ROWCOUNT,stepCt,'Done');
            commit;			
        
			--	update status in lz_src_analysis_metadata
			
			update tm_lz.lz_src_analysis_metadata
			set status='PRODUCTION'
			where etl_id = v_etl_id;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Set status to PRODUCTION in tm_lz.lz_src_analysis_metadata',SQL%ROWCOUNT,stepCt,'Done');
			commit;	
			
			--	delete data from biomart_stage
			
			delete from biomart_stage.bio_assay_analysis_gwas
			where bio_assay_analysis_id = v_bio_assay_analysis_id;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from biomart_stage.bio_assay_analysis_gwas',SQL%ROWCOUNT,stepCt,'Done');
			commit; 

			--	load top50 table
			i2b2_load_gwas_top50(v_bio_assay_analysis_id, jobId);
			
		else
			--	EQTL data
			v_EQTL_staged := 1;
			
			if v_eqtl_indx = 0 then
				--	disable indexes if loading eqtl data
				for eqtl_idx in (select index_name
									   ,table_name
								 from all_indexes 
					    		 where owner = 'BIOMART' 
								   and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50' )
				loop
					v_sqlText := 'alter index ' || eqtl_idx.index_name || ' unusable';
					stepCt := stepCt + 1;
					cz_write_audit(jobId,databaseName,procedureName,'Disabling index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,SQL%ROWCOUNT,stepCt,'Done');
					execute immediate(v_sqlText);
					stepCt := stepCt + 1;
					cz_write_audit(jobId,databaseName,procedureName,'Disabling complete',SQL%ROWCOUNT,stepCt,'Done');       
				end loop;
				v_eqtl_indx := 1;
			end if;
			
			--	delete existing data from bio_assay_analysis_eqtl
			delete from biomart.bio_assay_analysis_eqtl g
			where g.bio_assay_analysis_id = v_bio_assay_analysis_id;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Delete exising data for staged analyses from biomart.bio_assay_analysis_eqtl',SQL%ROWCOUNT,stepCt,'Done');
			commit;    

            insert into biomart.bio_assay_analysis_eqtl
            (bio_asy_analysis_eqtl_id
            ,bio_assay_analysis_id
            ,rs_id
            ,gene
            ,p_value
            ,p_value_char
            ,cis_trans
            ,distance_from_gene
            ,etl_id
            ,ext_data
            ,log_p_value)
            select bio_asy_analysis_eqtl_id
                  ,bio_assay_analysis_id
                  ,rs_id
                  ,gene
                  ,to_binary_double(p_value_char)
                  ,p_value_char
                  ,cis_trans
                  ,distance_from_gene
                  ,etl_id
                  ,case when length(ext_data)-length(replace(ext_data,';','')) < v_max_ext_flds
						then tm_cz.repeat_char(ext_data,v_max_ext_flds-(length(ext_data)-length(replace(ext_data,';',''))),';')
						else ext_data
				   end
                  ,log(10,to_binary_double(p_value_char))*-1
            from biomart_stage.bio_assay_analysis_eqtl
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis into biomart.bio_assay_analysis_eqtl',SQL%ROWCOUNT,stepCt,'Done');
            commit;        
			
			--	update data_count in bio_assay_analysis
			
            update biomart.bio_assay_analysis baa
			set data_count=(select count(*) from biomart.bio_assay_analysis_eqtl x
							where x.bio_assay_analysis_id=v_bio_assay_analysis_id) 
            where baa.bio_assay_analysis_id=v_bio_assay_analysis_id;
            stepCt := stepCt +1;
            cz_write_audit(jobId,databaseName,procedureName,'Update data_count for analysis',SQL%ROWCOUNT,stepCt,'Done');
            commit;			
        
			--	update status in lz_src_analysis_metadata
			
			update tm_lz.lz_src_analysis_metadata
			set status='PRODUCTION'
			where etl_id = v_etl_id;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Set status to PRODUCTION in tm_lz.lz_src_analysis_metadata',SQL%ROWCOUNT,stepCt,'Done');
			commit;	
			
			--	delete data from biomart_stage
			
			delete from biomart_stage.bio_assay_analysis_eqtl
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
            stepCt := stepCt + 1;
            cz_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from biomart_stage.bio_assay_analysis_eqtl',SQL%ROWCOUNT,stepCt,'Done');
            commit; 

			--	load top50 table
			i2b2_load_eqtl_top50(v_bio_assay_analysis_id, jobId);
		
		end if;
	end loop;
    
    --    rebuild indexes if loading GWAS data
    
    if v_GWAS_staged = 1 then
		for gwas_idx in (select index_name 
							   ,table_name
						 from all_indexes 
						 where owner = 'BIOMART' 
						   and table_name In ('BIO_ASSAY_ANALYSIS_GWAS','BIO_ASY_ANALYSIS_GWAS_TOP50') )
		loop
			v_sqlText := 'alter index ' || gwas_idx.index_name || ' rebuild';
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Rebuilding index ' || gwas_idx.index_name || ' on ' || gwas_idx.table_name,SQL%ROWCOUNT,stepCt,'Done');
			execute immediate(v_sqlText);
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Rebuilding complete',SQL%ROWCOUNT,stepCt,'Done');       
		end loop;
	end if;
	
	if v_gwas_staged = 0 and v_eqtl_staged = 0 then
        cz_write_audit(jobId, databaseName, procedureName, 'No staged data - run terminating normally',0,stepCt,'Done');
        cz_end_audit(jobId, 'Success');
	end if;

    --    rebuild indexes if loading EQTL data
    
    if v_eqtl_staged = 1 then
		for eqtl_idx in (select index_name 
							   ,table_name
						 from all_indexes 
						 where owner = 'BIOMART' 
						   and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50')
		loop
			v_sqlText := 'alter index ' || eqtl_idx.index_name || ' rebuild';
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Rebuilding index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,SQL%ROWCOUNT,stepCt,'Done');
			execute immediate(v_sqlText);
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Rebuilding complete',SQL%ROWCOUNT,stepCt,'Done');       
		end loop;
	end if;
	
	if v_eqtl_staged = 0 and v_eqtl_staged = 0 then
        cz_write_audit(jobId, databaseName, procedureName, 'No staged data - run terminating normally',0,stepCt,'Done');
        cz_end_audit(jobId, 'Success');
	end if;	
	--	check if any data left in staging tables, ususally indicated no bio_assay_analysis record in biomart
	
	select count(*) into v_exists
	from (select distinct bio_assay_analysis_id from biomart_stage.bio_assay_analysis_gwas
		  union 
		  select distinct bio_assay_analysis_id from biomart_stage.bio_assay_analysis_eqtl);
		  
	if v_exists = 0 then
        cz_write_audit(jobId, databaseName, procedureName, '**WARNING ** data remains in stage tables',0,stepCt,'Done');
        cz_end_audit(jobId, 'Success');
	end if;		  
		
    cz_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done');
    stepCt := stepCt + 1;
    
    cz_end_audit(jobId, 'Success');
    
    exception
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');
    
END;

