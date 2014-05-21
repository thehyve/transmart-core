--
-- Name: i2b2_move_analysis_to_prod(numeric, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_move_analysis_to_prod(i_etl_id numeric, i_job_id numeric DEFAULT 0) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
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
Declare
	
    --Audit variables
    newJobFlag     integer;
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         bigint;
    stepCt         integer;
	rowCt		integer;
	v_sqlerrm		varchar(1000);
	rtnCd			integer;
	errorNumber		character varying;
	errorMessage	character varying;
	
 
    v_etl_id					bigint;
    v_bio_assay_analysis_id		bigint;
    v_data_type					varchar(50);
    v_sqlText					varchar(2000);
    v_exists					integer;
    v_GWAS_staged				integer;
    v_EQTL_staged				integer;	
	v_max_ext_flds				integer;
    
	stage_rec					record;
 
BEGIN    
    jobId := i_job_id;
	
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := -1;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_MOVE_ANALYSIS_TO_PROD';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.czx_start_audit (procedureName, databaseName) into jobId;
	END IF;
        
    stepCt := 1;    
    select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Starting i2b2_move_analysis_to_prod',0,stepCt,'Done') into rtnCd;
	
	--	delete existing data for staged analyses from bio_asy_analysis_gwas
	
	begin
	delete from biomart.bio_assay_analysis_gwas g
	where g.bio_assay_analysis_id in
		  (select x.bio_assay_analysis_id
		   from tm_lz.lz_src_analysis_metadata t
			   ,biomart.bio_assay_analysis x
		  where t.status = 'STAGED'
			and t.data_type in ('GWAS','Metabolic GWAS')
			and t.study_id = x.etl_id
			and t.etl_id = x.etl_id_source
			and case when i_etl_id = -1 then 1
					 when t.etl_id = i_etl_id then 1
                     else 0 end = 1);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select * from tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete exising data for staged analyses from BIOMART.BIO_ASSAY_ANALYSIS_GWAS',rowCt,stepCt,'Done') into rtnCd;
	raise notice 'here';
	--	delete existing data for staged analyses from bio_asy_analysis_eqtl
	
	begin
	delete from biomart.bio_assay_analysis_eqtl g
	where g.bio_assay_analysis_id in
	     (select x.bio_assay_analysis_id
		  from tm_lz.lz_src_analysis_metadata t
			  ,biomart.bio_assay_analysis x
		  where t.status = 'STAGED'
			and t.data_type = 'EQTL'
			and t.study_id = x.etl_id
			and t.etl_id = x.etl_id_source
			and case when i_etl_id = -1 then 1
                     when t.etl_id = i_etl_id then 1
                     else 0 end = 1);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete exising data for staged analyses from BIOMART.BIO_ASSAY_ANALYSIS_EQTL',rowCt,stepCt,'Done') into rtnCd;
			
    --    load staged analysis to array
	
	v_GWAS_staged := 0;
    v_EQTL_staged := 0;
    
	for stage_rec in
		select baa.bio_assay_analysis_id
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
				   else 0 end = 1
	loop   
	
	    select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Loading ' || stage_rec.study_id || ' ' || stage_rec.orig_data_type || ' ' ||
                       stage_rec.analysis_name,0,stepCt,'Done') into rtnCd;
					   
		v_bio_assay_analysis_id := stage_rec.bio_assay_analysis_id;
		v_data_type := stage_rec.data_type;
		v_etl_id := stage_rec.etl_id;
		
		--	get max nbr fields in ext_data for original data type
			
		select max(field_idx) into v_max_ext_flds
		from biomart.bio_asy_analysis_data_idx
		where ext_type = stage_rec.orig_data_type;
					   
        if stage_rec.data_type = 'GWAS' then
            v_GWAS_staged := 1;

			--	move GWAS data from biomart_stage to biomart
			
			begin
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
                  ,cast(p_value_char as double precision)
                  ,p_value_char
                  ,etl_id
                  ,case when length(ext_data)-length(replace(ext_data,';','')) < v_max_ext_flds
						then ext_data || repeat(';',v_max_ext_flds-(length(ext_data)-length(replace(ext_data,';',''))))
						else ext_data
				   end
                  ,log(cast(p_value_char as double precision))*-1
            from biomart_stage.bio_assay_analysis_gwas
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
			get diagnostics rowCt := ROW_COUNT;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			end;
            stepCt := stepCt + 1;
            select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,rowCt,stepCt,'Done') into rtnCd;
			
			--	update data_count in bio_assay_analysis
			
			begin
		    update biomart.bio_assay_analysis baa
			set data_count=rowCt
            where baa.bio_assay_analysis_id=v_bio_assay_analysis_id;
			get diagnostics rowCt := ROW_COUNT;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			end;
            stepCt := stepCt +1;
            select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Update data_count in bio_assay_analysis',rowCt,stepCt,'Done') into rtnCd;
			
			v_sqlText := 'delete from biomart_stage.bio_assay_analysis_' || v_data_type || 
						 ' where bio_assay_analysis_id = ' || v_bio_assay_analysis_id;
			execute v_sqlText;
			get diagnostics rowCt := ROW_COUNT;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,rowCt,stepCt,'Done') into rtnCd;       
           
			--	load top 500 rows to bio_asy_analysis_gwas_top50
			
			-- select * from tm_cz.i2b2_load_gwas_top50(v_bio_assay_analysis_id,jobId) into rtnCd;
			
        end if;
        
        if stage_rec.data_type = 'EQTL' then
            v_EQTL_staged := 1;
			
			--	move EQTL data from biomart_stage to biomart

			begin
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
                  ,cast(p_value_char as double precision)
                  ,p_value_char
                  ,cis_trans
                  ,distance_from_gene
                  ,etl_id
                  ,case when length(ext_data)-length(replace(ext_data,';','')) < v_max_ext_flds
						then ext_data || repeat(';',v_max_ext_flds-(length(ext_data)-length(replace(ext_data,';',''))))
						else ext_data
				   end
                  ,log(cast(p_value_char as double precision))*-1
            from biomart_stage.bio_assay_analysis_eqtl
            where bio_assay_analysis_id = v_bio_assay_analysis_id;
			get diagnostics rowCt := ROW_COUNT;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			end;
            stepCt := stepCt + 1;
            select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,rowCt,stepCt,'Done') into rtnCd;
        
			--	update data_count in bio_assay_analysis
			
			begin
		    update biomart.bio_assay_analysis baa
			set data_count=rowCt
            where baa.bio_assay_analysis_id=v_bio_assay_analysis_id;
			get diagnostics rowCt := ROW_COUNT;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			end;
            stepCt := stepCt +1;
            select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Update data_count in bio_assay_analysis',rowCt,stepCt,'Done') into rtnCd;
           			
			v_sqlText := 'delete from biomart_stage.bio_assay_analysis_' || v_data_type || 
						 ' where bio_assay_analysis_id = ' || v_bio_assay_analysis_id;
			execute  v_sqlText;
			get diagnostics rowCt := ROW_COUNT;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from BIOMART_STAGE.BIO_ASSAY_ANALYSIS_' || v_data_type,rowCt,stepCt,'Done') into rtnCd;       

			--	load top 500 rows to bio_asy_analysis_eqtl_top50
			
			select * from tm_ca.i2b2_load_eqtl_top50(v_bio_assay_analysis_id,jobId) into rtnCd;
			
		end if;    
		
		--	update status in lz_src_analysis_metadata
		
		begin
        update tm_lz.lz_src_analysis_metadata
        set status='PRODUCTION'
        where etl_id = v_etl_id;
		get diagnostics rowCt := ROW_COUNT;
		exception
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			--Handle errors.
			select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			--End Proc
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return -16;
		end;
        stepCt := stepCt + 1;
        select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Set status to PRODUCTION in tm_lz.lz_src_analysis_metadata',rowCt,stepCt,'Done') into rtnCd;               
        
	end loop;
      
	--	check if no data loaded from biomart_stage, if none, terminate normally
	
	if v_GWAS_staged = 0 and v_EQTL_staged = 0 then
	    stepCt := stepCt + 1;
        select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No staged data - run terminating normally',0,stepCt,'Done') into rtnCd;
		return 0;
	end if;
    
	--	check if any data left in stage tables after move, usually indicates missing bio_assay_analysis record
	
	if i_etl_id = -1 then
		select count(*) into v_exists
		from (select distinct bio_assay_analysis_id from biomart_stage.bio_assay_analysis_gwas
			  union
			  select distinct bio_assay_analysis_id from biomart_stage.bio_assay_analysis_eqtl) x;
		if v_exists > 0 then
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'**WARNING ** Data remains in staging tables',0,stepCt,'Done') into rtnCd;
		end if;
	end if;
    
    select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'End i2b2_move_analysis_to_prod',0,stepCt,'Done') into rtnCd;
    stepCt := stepCt + 1;
    
    select tm_cz.czx_end_audit(jobId, 'Success') into rtnCd;
	return 0;
    
    
END;
$$;

