--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_SECURE_STUDY
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_SECURE_STUDY" 
(trial_id	 varchar2
,currentJobID NUMBER := null
)
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research d, LLC.
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

	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0);
	stepCt number(18,0);
	
	v_bio_experiment_id	number(18,0);
	pExists				int;
	TrialId				varchar2(100);

BEGIN

	TrialId := trial_id;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		cz_start_audit (procedureName, databaseName, jobID);
	END IF;
  
	stepCt := 0;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Start ' || procedureName,0,stepCt,'Done');
	
	--	create security records in observation_fact
	
	i2b2_create_security_for_trial(TrialId, 'Y', jobID);
	
	--	load i2b2_secure
	
	i2b2_load_security_data(jobID);
	
	--	check if entry exists for study in bio_experiment
	
	select count(*) into pExists
	from biomart.bio_experiment
	where accession = TrialId;
	
	if pExists = 0 then
		insert into biomart.bio_experiment
		(title, accession, etl_id)
		select 'Metadata not available'
			  ,TrialId
			  ,'METADATA:' || TrialId
		from dual;
	    stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Insert trial/study into biomart.bio_experiment',SQL%ROWCOUNT,stepCt,'Done');
		commit;
	end if;
	
	select bio_experiment_id into v_bio_experiment_id
	from biomart.bio_experiment
	where accession = TrialId;
	
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
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Inserted trial/study into SEARCHAPP search_secure_object',SQL%ROWCOUNT,stepCt,'Done');
	commit;
		
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,SQL%ROWCOUNT,stepCt,'Done');
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
	
END;
/
 
