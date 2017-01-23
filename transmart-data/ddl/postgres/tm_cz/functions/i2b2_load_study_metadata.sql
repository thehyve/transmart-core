--
-- Name: i2b2_load_study_metadata(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_study_metadata(currentjobid numeric DEFAULT (-1)) RETURNS bigint
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
declare
  
	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
	rtnCd			integer;
	
	dcount 				int;
	lcount 				int;
	upload_date			timestamp;
	tmp_compound		varchar(200);
	tmp_disease			varchar(200);
	tmp_organism		varchar(200);
	tmp_pubmed			varchar(2000);
	pubmed_id			varchar(200);
	pubmed_title		varchar(2000);
	
	study_compound_rec	record;
	study_disease_rec	record;
	study_taxonomy_rec  record;
	study_pubmed_rec 	record;

BEGIN

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;
	databaseName := 'tm_cz';
	procedureName := 'i2b2_load_study_metadata';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it

	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.czx_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := 0;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done') into rtnCd;

	select clock_timestamp() into upload_date;
 
	--	Update existing bio_experiment data
	
	begin
	with upd as (select m.study_id
				,m.title
				,m.description
				,m.design
				,case when tm_cz.is_date(m.start_date,'YYYYMMDD') = 1 then null
					  else to_date(m.start_date,'YYYYMMDD') end as start_date
				,case when tm_cz.is_date(m.completion_date,'YYYYMMDD') = 1 then null
					  else to_date(m.completion_date,'YYYYMMDD') end as completion_date
				,coalesce(m.primary_investigator,m.study_owner) as primary_investigator
				,m.overall_design
				,m.institution
				,m.country
				from tm_lz.lt_src_study_metadata m
				where m.study_id is not null)
	update biomart.bio_experiment b
	set title=upd.title
	    ,description=upd.description
		,design=upd.design
		,start_date=upd.start_date
		,completion_date=upd.completion_date
		,primary_investigator=upd.primary_investigator
		,overall_design=upd.overall_design
		,institution=upd.institution
		,country=upd.country 
	from upd
	where b.accession = upd.study_id
	  and b.etl_id = 'METADATA:' || upd.study_id;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Updated trial data in BIOMART bio_experiment',rowCt,stepCt,'Done') into rtnCd;

/*	
	--	Update existing bio_clinical_trial data only for true Clinical Trials or JnJ Experimental Medicine Studies

	update biomart.bio_clinical_trial b
	set (study_owner
	    ,study_phase
		,blinding_procedure
		,studytype
		,duration_of_study_weeks
		,number_of_patients
		,number_of_sites
		,route_of_administration
		,dosing_regimen
		,group_assignment
		,type_of_control
		,completion_date
		,primary_end_points
		,secondary_end_points
		,inclusion_criteria
		,exclusion_criteria
		,subjects
		,gender_restriction_mfb
		,min_age
		,max_age
		,secondary_ids
		,development_partner
		,main_findings
		,geo_platform
		--,platform_name
		,search_area
        ) =
		(select m.study_owner
			   ,m.study_phase
			   ,m.blinding_procedure
			   ,m.studytype
			   ,decode(is_number(m.duration_of_study_weeks),1,null,to_number(m.duration_of_study_weeks))
			   ,decode(is_number(m.number_of_patients),1,null,to_number(m.number_of_patients))
			   ,decode(is_number(m.number_of_sites),1,null,to_number(m.number_of_sites))
			   ,m.route_of_administration
			   ,m.dosing_regimen
			   ,m.group_assignment
			   ,m.type_of_control
			   ,decode(is_date(m.completion_date,'YYYYMMDD'),1,null,to_date(m.completion_date,'YYYYMMDD'))
			   ,m.primary_end_points
			   ,m.secondary_end_points
			   ,m.inclusion_criteria
			   ,m.exclusion_criteria
			   ,m.subjects
			   ,m.gender_restriction_mfb
			   ,decode(is_number(m.min_age),1,null,to_number(m.min_age))
			   ,decode(is_number(m.max_age),1,null,to_number(m.max_age))
			   ,m.secondary_ids
			   ,m.development_partner
			   ,m.main_findings
			   ,m.geo_platform
			   --,m.platform_name
			   ,m.search_area
		 from lt_src_study_metadata m
		 where m.study_id is not null
		   and b.trial_number = m.study_id
		)
	where exists
	     (select 1 from lt_src_study_metadata x
		  where b.trial_number = x.study_id
		    and x.study_id is not null
		 )
	;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated study data in BIOMART bio_clinical_trial',SQL%ROWCOUNT,stepCt,'Done');
	commit;
*/
	
	--	Add new trial data to bio_experiment
	
	begin
	insert into biomart.bio_experiment
	(bio_experiment_type
	,title
	,description
	,design
	,start_date
	,completion_date
	,primary_investigator
	,contact_field
	,etl_id
	,status
	,overall_design
	,accession
	,country
	,institution)
	select 'Experiment'
	      ,m.title
		  ,m.description
		  ,m.design
		  ,case when tm_cz.is_date(m.start_date,'YYYYMMDD') = 1 then null
				else to_date(m.start_date,'YYYYMMDD') end as start_date
		  ,case when tm_cz.is_date(m.completion_date,'YYYYMMDD') = 1 then null
				else to_date(m.completion_date,'YYYYMMDD') end as completion_date
		  ,coalesce(m.primary_investigator,m.study_owner) as primary_investigator
		  ,m.contact_field
		  ,'METADATA:' || m.study_id
		  ,m.study_id
		  ,m.overall_design
		  ,m.study_id
		  ,m.country
		  ,m.institution
	from tm_lz.lt_src_study_metadata m
	where m.study_id is not null
	  and not exists
	      (select 1 from biomart.bio_experiment x
		   where m.study_id = x.accession
		     and m.study_id is not null);
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
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study to BIOMART bio_experiment',rowCt,stepCt,'Done') into rtnCd;

/*		
	--	Add new trial data to bio_clinical_trial
	
	insert into biomart.bio_clinical_trial
	(trial_number
	,study_owner
	,study_phase
	,blinding_procedure
	,studytype
	,duration_of_study_weeks
	,number_of_patients
	,number_of_sites
	,route_of_administration
	,dosing_regimen
	,group_assignment
	,type_of_control
	,completion_date
	,primary_end_points
	,secondary_end_points
	,inclusion_criteria
	,exclusion_criteria
	,subjects
	,gender_restriction_mfb
	,min_age
	,max_age
	,secondary_ids
	,bio_experiment_id
	,development_partner
	,main_findings
	,geo_platform
	--,platform_name
	,search_area
	)
	select m.study_id
          ,m.study_owner
          ,m.study_phase
          ,m.blinding_procedure
          ,m.studytype
		  ,decode(is_number(m.duration_of_study_weeks),1,null,to_number(m.duration_of_study_weeks))
		  ,decode(is_number(m.number_of_patients),1,null,to_number(m.number_of_patients))
		  ,decode(is_number(m.number_of_sites),1,null,to_number(m.number_of_sites))
          ,m.route_of_administration
          ,m.dosing_regimen
          ,m.group_assignment
          ,m.type_of_control
          ,decode(is_date(m.completion_date,'YYYYMMDD'),1,null,to_date(m.completion_date,'YYYYMMDD'))
          ,m.primary_end_points
          ,m.secondary_end_points
          ,m.inclusion_criteria
          ,m.exclusion_criteria
          ,m.subjects
          ,m.gender_restriction_mfb
		  ,decode(is_number(m.min_age),1,null,to_number(m.min_age))
		  ,decode(is_number(m.max_age),1,null,to_number(m.max_age))
          ,m.secondary_ids
          ,b.bio_experiment_id
		  ,m.development_partner
		  ,m.main_findings
		  ,m.geo_platform
		  --,m.platform_name
		  ,m.search_area
	from lt_src_study_metadata m
	    ,biomart.bio_experiment b
	where m.study_id is not null
	  and m.study_id = b.accession
	  and not exists
	      (select 1 from biomart.bio_clinical_trial x
		   where m.study_id = x.trial_number);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Inserted trial data in BIOMART bio_clinical_trial',SQL%ROWCOUNT,stepCt,'Done');
	commit;
*/
	
	--	Insert new trial into bio_data_uid
	
	begin
	insert into biomart.bio_data_uid
	(bio_data_id
	,unique_id
	,bio_data_type
	)
	select distinct b.bio_experiment_id
	      ,'EXP:' || m.study_id
		  ,'EXP'
	from biomart.bio_experiment b
		,tm_lz.lt_src_study_metadata m
	where m.study_id is not null
	  and m.study_id = b.accession
	  and not exists
	      (select 1 from biomart.bio_data_uid x
		   where x.unique_id = 'EXP:' || m.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Added study to bio_data_uid',rowCt,stepCt,'Done') into rtnCd;

	--	delete existing compound data for study, compound list may change
	
	begin
	delete from biomart.bio_data_compound dc
	where dc.bio_data_id in 
		 (select x.bio_experiment_id
		  from biomart.bio_experiment x
			  ,tm_lz.lt_src_study_metadata y
		  where x.accession = y.study_id
		    and x.etl_id = 'METADATA:' || y.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete existing study data from bio_compound',rowCt,stepCt,'Done') into rtnCd;

	--	add study compound data
	
	for study_compound_rec in
		select distinct study_id
			  ,compound
		from tm_lz.lt_src_study_metadata
		where compound is not null
	loop
		select length(study_compound_rec.compound)-length(replace(study_compound_rec.compound,';',''))+1 into dcount;
		while dcount > 0
		Loop	
			select tm_cz.parse_nth_value(study_compound_rec.compound,dcount,';') into tmp_compound;
			   
			--	add new compound
			begin
			insert into biomart.bio_compound
			(generic_name)
			select tmp_compound
			where not exists
				 (select 1 from biomart.bio_compound x
				  where upper(x.generic_name) = upper(tmp_compound))
			  and tmp_compound is not null;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study compound to bio_compound',rowCt,stepCt,'Done') into rtnCd;
					
			--	Insert new trial data into bio_data_compound
			begin
			insert into biomart.bio_data_compound
			(bio_data_id
			,bio_compound_id
			,etl_source
			)
			select b.bio_experiment_id
				  ,c.bio_compound_id
				  ,'METADATA:' || study_compound_rec.study_id
			from biomart.bio_experiment b
				,biomart.bio_compound c
			where upper(tmp_compound) = upper(c.generic_name) 
			  and tmp_compound is not null
			  and b.accession = study_compound_rec.study_id
			  and not exists
					 (select 1 from biomart.bio_data_compound x
						  where b.bio_experiment_id = x.bio_data_id
							and c.bio_compound_id = x.bio_compound_id);
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study compound to bio_data_compound',rowCt,stepCt,'Done') into rtnCd;			
			dcount := dcount - 1;
		end loop;
	end loop;

	--	delete existing disease data for studies
	
	begin
	delete from biomart.bio_data_disease dc
	where dc.bio_data_id in 
		 (select x.bio_experiment_id
		  from biomart.bio_experiment x
			  ,tm_lz.lt_src_study_metadata y
		  where x.accession = y.study_id
		    and x.etl_id = 'METADATA:' || y.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete existing study data from bio_data_disease',rowCt,stepCt,'Done') into rtnCd;

	--	add study disease data
	
	for study_disease_rec in
		select distinct study_id, disease
		from tm_lz.lt_src_study_metadata
		where disease is not null
	loop
		select length(study_disease_rec.disease)-length(replace(study_disease_rec.disease,';',''))+1 into dcount;
		while dcount > 0
		Loop	
			select tm_cz.parse_nth_value(study_disease_rec.disease,dcount,';') into tmp_disease;
			   
			--	add new disease
			begin
			insert into biomart.bio_disease
			(disease
			,prefered_name)
			select tmp_disease
				  ,tmp_disease
			where not exists
				 (select 1 from biomart.bio_disease x
				  where upper(x.disease) = upper(tmp_disease))
			  and tmp_disease is not null;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study disease to bio_disease',rowCt,stepCt,'Done') into rtnCd;
			
			--	Insert new trial data into bio_data_disease
			begin
			insert into biomart.bio_data_disease
			(bio_data_id
			,bio_disease_id
			,etl_source
			)
			select b.bio_experiment_id
				  ,c.bio_disease_id
				  ,'METADATA:' || study_disease_rec.study_id
			from biomart.bio_experiment b
				,biomart.bio_disease c
			where upper(tmp_disease) = upper(c.disease) 
			  and tmp_disease is not null
			  and b.accession = study_disease_rec.study_id
			  and not exists
					 (select 1 from biomart.bio_data_disease x
					  where b.bio_experiment_id = x.bio_data_id
						and c.bio_disease_id = x.bio_disease_id);
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study disease to bio_data_disease',rowCt,stepCt,'Done') into rtnCd;
			dcount := dcount - 1;
		end loop;
	end loop;

	--	delete existing taxonomy data for studies
	
	begin
	delete from biomart.bio_data_taxonomy dc
	where dc.bio_data_id in 
		 (select x.bio_experiment_id
		  from biomart.bio_experiment x
			  ,tm_lz.lt_src_study_metadata y
		  where x.accession = y.study_id
		    and x.etl_id = 'METADATA:' || y.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete existing study data from bio_data_taxonomy',rowCt,stepCt,'Done') into rtnCd;

	--	add study organism to taxonomy
	
	for study_taxonomy_rec in
		select distinct study_id, organism
		from tm_lz.lt_src_study_metadata
		where organism is not null
	loop
		select length(study_taxonomy_rec.organism)-length(replace(study_taxonomy_rec.organism,';',''))+1 into dcount;
		while dcount > 0
		Loop	
			select tm_cz.parse_nth_value(study_taxonomy_rec.organism,dcount,';') into tmp_organism;
			   
			--	add new organism
			begin
			insert into biomart.bio_taxonomy
			(taxon_name
			,taxon_label)
			select tmp_organism
				  ,tmp_organism
			where not exists
				 (select 1 from biomart.bio_taxonomy x
				  where upper(x.taxon_name) = upper(tmp_organism))
			  and tmp_organism is not null;
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study organism to bio_taxonomy',rowCt,stepCt,'Done') into rtnCd;
							
			--	Insert new trial data into bio_data_taxonomy
			begin
			insert into biomart.bio_data_taxonomy
			(bio_data_id
			,bio_taxonomy_id
			,etl_source
			)
			select b.bio_experiment_id
				  ,c.bio_taxonomy_id
				  ,'METADATA:' || study_disease_rec.study_id
			from biomart.bio_experiment b
				,biomart.bio_taxonomy c
			where upper(tmp_organism) = upper(c.taxon_name) 
			  and tmp_organism is not null
			  and b.accession = study_disease_rec.study_id
			  and not exists
					 (select 1 from biomart.bio_data_taxonomy x
					  where b.bio_experiment_id = x.bio_data_id
						and c.bio_taxonomy_id = x.bio_taxonomy_id);
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study organism to bio_data_taxonomy',rowCt,stepCt,'Done') into rtnCd;

			dcount := dcount - 1;
		end loop;
	end loop;
	
	--	add ncbi/GEO linking
	
	--	check if ncbi exists in bio_content_repository, if not, add
	
	select count(*) into dcount
	from biomart.bio_content_repository
	where repository_type = 'NCBI'
	  and location_type = 'URL';
	
	if dcount = 0 then
		begin
		insert into biomart.bio_content_repository
		(location
		,active_y_n
		,repository_type
		,location_type) 
		values ('http://www.ncbi.nlm.nih.gov/','Y','NCBI','URL');
		exception
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			--Handle errors.
			select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			--End Proc
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return -16;
		get diagnostics rowCt := ROW_COUNT;	
		end;
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Insert link to NCBI into bio_content_repository',rowCt,stepCt,'Done') into rtnCd;
	end if;

	--	insert GSE studies into bio_content
	
	begin
	insert into biomart.bio_content
	(repository_id
	,location
	,file_type
	,etl_id_c
	)
	select bcr.bio_content_repo_id
		  ,'geo/query/acc.cgi?acc=' || m.study_id
		  ,'Experiment Web Link'
		  ,'METADATA:' || m.study_id
	from tm_lz.lt_src_study_metadata m
		,biomart.bio_content_repository bcr
	where m.study_id like 'GSE%'
	  and bcr.repository_type = 'NCBI'
	  and bcr.location_type = 'URL'
	  and not exists
		 (select 1 from biomart.bio_content x
		  where x.etl_id_c like '%' || m.study_id || '%'
		    and x.file_type = 'Experiment Web Link'
			and x.location = 'geo/query/acc.cgi?acc=' || m.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add GEO study to bio_cotent',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert GSE studies into bio_content_reference
	
	begin
	insert into biomart.bio_content_reference
	(bio_content_id
	,bio_data_id
	,content_reference_type
	,etl_id_c
	)
	select bc.bio_file_content_id
		  ,be.bio_experiment_id
		  ,'Experiment Web Link'
		  ,'METADATA:' || m.study_id
	from tm_lz.lt_src_study_metadata m
		,biomart.bio_experiment be
		,biomart.bio_content bc
	where m.study_id like 'GSE%'
	  and m.study_id = be.accession
	  and bc.file_type = 'Experiment Web Link'
	  and bc.etl_id_c = 'METADATA:' || m.study_id
	  and bc.location = 'geo/query/acc.cgi?acc=' || m.study_id
	  and not exists
		 (select 1 from biomart.bio_content_reference x
		  where bc.bio_file_content_id = x.bio_content_id
		    and be.bio_experiment_id = x.bio_data_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Added GEO study to bio_content_reference',rowCt,stepCt,'Done') into rtnCd;

	--	add PUBMED linking
	
	--	delete existing pubmed data for studies
	
	begin
	delete from biomart.bio_content_reference dc
	where dc.bio_content_id in 
		 (select x.bio_file_content_id
		  from biomart.bio_content x
			  ,tm_lz.lt_src_study_metadata y
		  where x.file_type = 'Publication Web Link'
		    and x.etl_id_c = 'METADATA:' || y.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete existing study pubmed from bio_content_reference',rowCt,stepCt,'Done') into rtnCd;
		
	begin
	delete from biomart.bio_content dc
	where dc.bio_file_content_id in 
		 (select x.bio_file_content_id
		  from biomart.bio_content x
			  ,tm_lz.lt_src_study_metadata y
		  where x.file_type = 'Publication Web Link'
		    and x.etl_id_c = 'METADATA:' || y.study_id);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete existing study pubmed from bio_content',rowCt,stepCt,'Done') into rtnCd;

	--	add study pubmed ids'
	
	select count(*) into dcount
	from biomart.bio_content_repository
	where repository_type = 'PubMed';	
	
	if dcount = 0 then
		begin
		insert into biomart.bio_content_repository
		(location
		,active_y_n
		,repository_type
		,location_type) 
		values ('http://www.ncbi.nlm.nih.gov/pubmed/','Y','PubMed','URL');
		exception
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			--Handle errors.
			select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			--End Proc
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return -16;
		get diagnostics rowCt := ROW_COUNT;	
		end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add pubmed url to bio_content_repository',rowCt,stepCt,'Done') into rtnCd;
	end if;
	
	for study_pubmed_rec in
		select distinct study_id, pubmed_ids
		from tm_lz.lt_src_study_metadata
		where pubmed_ids is not null
	loop
		select length(study_pubmed_rec.pubmed_ids)-length(replace(study_pubmed_rec.pubmed_ids,'|',''))+1 into dcount;
		while dcount > 0
		Loop	
			-- multiple pubmed id can be separated by |, pubmed id and title are separated by :
			
			select tm_cz.parse_nth_value(study_pubmed_rec.pubmed_ids,dcount,'|') into tmp_pubmed;			
			select tm_cz.instr(tmp_pubmed,'@') into lcount;
			
			if lcount = 0 then
				pubmed_id := tmp_pubmed;
				pubmed_title := null;
			else
				pubmed_id := substr(tmp_pubmed,1,instr(tmp_pubmed,'@')-1);	
				pubmed_title := substr(tmp_pubmed,instr(tmp_pubmed,'@')+1);
			end if;
			
			begin
			insert into biomart.bio_content
			(repository_id
			,location
			,title
			,file_type
			,etl_id_c
			)
			select bcr.bio_content_repo_id
				  ,pubmed_id
				  ,pubmed_title
				  ,'Publication Web Link'
				  ,'METADATA:' || study_pubmed_rec.study_id
			from biomart.bio_content_repository bcr
			where bcr.repository_type = 'PubMed'
			  and not exists
					 (select 1 from biomart.bio_content x
					  where x.etl_id_c like '%' || study_pubmed_rec.study_id || '%'
					    and x.file_type = 'Publication Web Link'
						and x.location = pubmed_id);
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study pubmed to bio_content',rowCt,stepCt,'Done') into rtnCd;
		
			begin
			insert into biomart.bio_content_reference
			(bio_content_id
			,bio_data_id
			,content_reference_type
			,etl_id_c
			)
			select bc.bio_file_content_id
				  ,be.bio_experiment_id
				  ,'Publication Web Link'
				  ,'METADATA:' || study_pubmed_rec.study_id
			from biomart.bio_experiment be
				,biomart.bio_content bc
			where be.accession = study_pubmed_rec.study_id
			  and bc.file_type = 'Publication Web Link'
			  and bc.etl_id_c = 'METADATA:' || study_pubmed_rec.study_id
			  and bc.location = pubmed_id
			  and not exists
				 (select 1 from biomart.bio_content_reference x
				  where bc.bio_file_content_id = x.bio_content_id
					and be.bio_experiment_id = x.bio_data_id);	
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;
			get diagnostics rowCt := ROW_COUNT;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study pubmed to bio_content_reference',rowCt,stepCt,'Done') into rtnCd;
			dcount := dcount - 1;
		end loop;
	end loop;
	
		--	Create i2b2_tags

	begin
	delete from i2b2metadata.i2b2_tags
	where upper(tag_type) = 'Trial';
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Delete study from i2b2_tags',rowCt,stepCt,'Done') into rtnCd;

	begin
	insert into i2b2metadata.i2b2_tags
	(tag_id, path, tag, tag_type, tags_idx)
	select nextval('i2b2metadata.sq_i2b2_tag_id')
		  ,min(b.c_fullname) as path
		  ,be.accession as tag
		  ,'Trial' as tag_type
		  ,0 as tags_idx
	from biomart.bio_experiment be
		,i2b2metadata.i2b2 b
	where be.accession = b.sourcesystem_cd
	group by be.accession;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Add study to i2b2_tags',rowCt,stepCt,'Done') into rtnCd;

/*					 
	--	Insert trial data tags - COMPOUND
	
	delete from i2b2_tags t
	where upper(t.tag_type) = 'COMPOUND';

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing Compound tags in I2B2METADATA i2b2_tags',SQL%ROWCOUNT,stepCt,'Done');
	commit;	
	
	insert into i2b2_tags
	(path, tag, tag_type, tags_idx)
	select distinct min(o.c_fullname) as path
		  ,decode(x.rec_num,1,c.generic_name,c.brand_name) as tag
		  ,'Compound' as tag_type
		  ,1 as tags_idx
	from bio_experiment be
		,bio_data_compound bc
		,bio_compound c
		,i2b2 o
		,(select rownum as rec_num from table_access where rownum < 3) x
	where be.bio_experiment_id = bc.bio_data_id
       and bc.bio_compound_id = c.bio_compound_id
       and be.accession = o.sourcesystem_cd
       and decode(x.rec_num,1,c.generic_name,c.brand_name) is not null
	group by decode(x.rec_num,1,c.generic_name,c.brand_name);

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert Compound tags in I2B2METADATA i2b2_tags',SQL%ROWCOUNT,stepCt,'Done');
	commit;	
					 
	--	Insert trial data tags - DISEASE
	
	delete from i2b2_tags t
	where upper(t.tag_type) = 'DISEASE';

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing DISEASE tags in I2B2METADATA i2b2_tags',SQL%ROWCOUNT,stepCt,'Done');
	commit;	
		
	insert into i2b2_tags
	(path, tag, tag_type, tags_idx)
	select distinct min(o.c_fullname) as path
		   ,c.prefered_name
		   ,'Disease' as tag_type
		   ,1 as tags_idx
	from bio_experiment be
		,bio_data_disease bc
		,bio_disease c
		,i2b2 o
      --,(select rownum as rec_num from table_access where rownum < 3) x
	where be.bio_experiment_id = bc.bio_data_id
      and bc.bio_disease_id = c.bio_disease_id
      and be.accession = o.sourcesystem_cd
    --and decode(x.rec_num,1,c.generic_name,c.brand_name) is not null
	group by c.prefered_name;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert Disease tags in I2B2METADATA i2b2_tags',SQL%ROWCOUNT,stepCt,'Done');
	commit;	
*/
	
    ---Cleanup OVERALL JOB if this proc is being run standalone

	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done') into rtnCd;

	---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.czx_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;
	
END;

$$;

