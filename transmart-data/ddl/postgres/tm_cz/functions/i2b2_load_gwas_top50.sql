--
-- Name: i2b2_load_gwas_top50(numeric, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_gwas_top50(i_bio_assay_analysis_id numeric, currentjobid numeric DEFAULT 0) RETURNS integer
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
	newJobFlag integer;
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID bigint;
	stepCt integer;
	rowCt		integer;
	tExists		integer;
	v_sqlerrm		varchar(1000);
	rtnCd		integer;
	errorNumber		character varying;
	errorMessage	character varying;
	
begin

	stepCt := 0;
	rowCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_GWAS_TOP50';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.czx_start_audit (procedureName, databaseName) into jobId;
	END IF;
	
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Start ' || procedureName,0,stepCt,'Done') into rtnCd;
	
	begin
	delete from biomart.bio_asy_analysis_gwas_top50
	where bio_assay_analysis_id = i_bio_assay_analysis_id;
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
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete analysis from bio_asy_analysis_gwas_top50',rowCt,stepCt,'Done') into rtnCd;
		
	begin
	insert into biomart.bio_asy_analysis_gwas_top50
	(bio_assay_analysis_id
	,analysis
	,chrom
	,pos
	,rsgene
	,rsid
	,pvalue
	,logpvalue
	,extdata
	,rnum
	)
	select a.*
	from (select gwas.bio_assay_analysis_id
				,baa.analysis_name as analysis
				,info.chrom as chrom
				,info.pos as pos
				,info.gene_name as rsgene
				,gwas.rs_id as rsid
				,gwas.p_value as pvalue
				,gwas.log_p_value as logpvalue
				,gwas.ext_data as extdata
				,row_number () over (order by gwas.p_value asc, gwas.rs_id asc) as rnum
		  from biomart.bio_assay_analysis_gwas gwas 
		  inner join biomart.bio_assay_analysis baa 
				on  baa.bio_assay_analysis_id = gwas.bio_assay_analysis_id
		  inner join deapp.de_rc_snp_info info 
				on  gwas.rs_id = info.rs_id 
				and hg_version='19'
		  where gwas.bio_assay_analysis_id = i_bio_assay_analysis_id) a
	where a.rnum <= 500;
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
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Insert into bio_asy_analysis_gwas_top50',rowCt,stepCt,'Done') into rtnCd;
	
      ---Cleanup OVERALL JOB if this proc is being run standalone
	if newjobflag = 1
	then
		select tm_cz.czx_end_audit (jobID, 'SUCCESS') into rtnCd;
	end if;
	
	return 0;

END;
$$;

