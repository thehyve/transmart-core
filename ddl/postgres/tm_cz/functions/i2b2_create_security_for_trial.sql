--
-- Name: i2b2_create_security_for_trial(character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_create_security_for_trial(trial_id character varying, secured_study character varying DEFAULT 'N'::character varying, currentjobid numeric DEFAULT (-1)) RETURNS numeric
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
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
	rtnCd			numeric;

	TrialID varchar(100);
	securedStudy varchar(5);
	pExists			integer;
	v_bio_experiment_id	numeric(18,0);

BEGIN
	TrialID := trial_id;
	securedStudy := secured_study;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_CREATE_SECURITY_FOR_TRIAL';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobId;
	END IF;

	stepCt := 0;
  
	--	insert patients to patient_trial table
	
	begin
	delete from i2b2demodata.patient_trial
	where trial  = TrialID;
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2DEMODATA patient_trial',rowCt,stepCt,'Done') into rtnCd;

	begin
	insert into i2b2demodata.patient_trial
	(patient_num
	,trial
	,secure_obj_token
	)
	select patient_num, 
		   TrialID,
		   case when securedStudy = 'N' then 'EXP:PUBLIC' else 'EXP:' || trialID end
	from i2b2demodata.patient_dimension
	where sourcesystem_cd like TrialID || ':%';
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial into I2B2DEMODATA patient_trial',rowCt,stepCt,'Done') into rtnCd;
	
	--	if secure study, then create bio_experiment record if needed and insert to search_secured_object
	
	select count(*) into pExists
	from searchapp.search_secure_object sso
	where bio_data_unique_id = 'EXP:' || TrialId;
	
	if pExists = 0 then
		--	if securedStudy = Y, add trial to searchapp.search_secured_object
		if securedStudy = 'Y' then
			select count(*) into pExists
			from biomart.bio_experiment
			where accession = TrialId;
			
			if pExists = 0 then
				begin
				insert into biomart.bio_experiment
				(title, accession, etl_id)
				select 'Metadata not available'
					  ,TrialId
					  ,'METADATA:' || TrialId;
				get diagnostics rowCt := ROW_COUNT;	
				exception
				when others then
					errorNumber := SQLSTATE;
					errorMessage := SQLERRM;
					--Handle errors.
					select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
					--End Proc
					select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
					return -16;	
				end;
				stepCt := stepCt + 1;
				select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert trial/study into biomart.bio_experiment',rowCt,stepCt,'Done') into rtnCd;
			end if;
			
			select bio_experiment_id into v_bio_experiment_id
			from biomart.bio_experiment
			where accession = TrialId;
			
			begin
			insert into searchapp.search_secure_object
			(bio_data_id
			,display_name
			,data_type
			,bio_data_unique_id
			)
			select v_bio_experiment_id
				  ,parse_nth_value(md.c_fullname,2,'\') || ' - ' || md.c_name as display_name
				  ,'BIO_CLINICAL_TRIAL' as data_type
				  ,'EXP:' || TrialId as bio_data_unique_id
			from i2b2metadata.i2b2 md
			where md.sourcesystem_cd = TrialId
			  and md.c_hlevel = 
				 (select min(x.c_hlevel) from i2b2metadata.i2b2 x
				  where x.sourcesystem_cd = TrialId)
			  and not exists
				 (select 1 from searchapp.search_secure_object so
				  where v_bio_experiment_id = so.bio_data_id);
			get diagnostics rowCt := ROW_COUNT;	
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted trial/study into SEARCHAPP search_secure_object',rowCt,stepCt,'Done') into rtnCd;
		end if;
	else
		--	if securedStudy = N, delete entry from searchapp.search_secure_object
		if securedStudy = 'N' then
			begin
			delete from searchapp.search_secure_object
			where bio_data_unique_id = 'EXP:' || TrialId;
			get diagnostics rowCt := ROW_COUNT;	
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				--Handle errors.
				select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
				select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
				return -16;	
			end;
			stepCt := stepCt + 1;
			select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Deleted trial/study from SEARCHAPP search_secure_object',rowCt,stepCt,'Done') into rtnCd;
		end if;		
	end if;
     
 
    ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;
	
	EXCEPTION
	WHEN OTHERS THEN
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;

END;

$$;

