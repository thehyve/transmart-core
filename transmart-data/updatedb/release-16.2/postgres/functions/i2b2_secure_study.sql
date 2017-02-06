--
-- Name: i2b2_secure_study(text, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--

SET check_function_bodies = false;
SET search_path = tm_cz, pg_catalog;

CREATE OR REPLACE FUNCTION i2b2_secure_study(trial_id text, currentjobid bigint DEFAULT NULL::bigint) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

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
	newJobFlag integer;
	databaseName varchar(100);
	procedureName varchar(100);
	jobID bigint;
	stepCt bigint;
	rowCt bigint;

	v_bio_experiment_id	bigint;
	pExists				integer;
	TrialId				varchar(100);

	rtnCd numeric;

BEGIN

	TrialId := trial_id;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'i2b2_secure_study';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(coalesce(jobID::text, '') = '' or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select cz_start_audit (procedureName, databaseName) into jobID;
	END IF;
  
	stepCt := 0;
	stepCt := stepCt + 1;
	select cz_write_audit(jobID,databaseName,procedureName,'Start ' || procedureName,0,stepCt,'Done') into rtnCd;
	
	--	create security records in observation_fact
	
	select i2b2_create_security_for_trial(TrialId, 'Y', jobID) into rtnCd;
	
	--	load i2b2_secure
	
	select i2b2_load_security_data(jobID) into rtnCd;
	
	--	check if entry exists for study in bio_experiment
	
	select count(*) into pExists
	from biomart.bio_experiment
	where accession = TrialId;
	
	if pExists = 0 then
		insert into biomart.bio_experiment
		(title, accession, etl_id)
		          'Metadata not available'
			  ,TrialId
			  ,'METADATA:' || TrialId
		;
		get diagnostics rowCt := ROW_COUNT;
		stepCt := stepCt + 1;
		select cz_write_audit(jobID,databaseName,procedureName,'Insert trial/study into biomart.bio_experiment',rowCt,stepCt,'Done') into rtnCd;
		--commit;
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
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select cz_write_audit(jobID,databaseName,procedureName,'Inserted trial/study into SEARCHAPP search_secure_object',rowCt,stepCt,'Done') into rtnCd;
	--commit;
		
	stepCt := stepCt + 1;
	select cz_write_audit(jobID,databaseName,procedureName,'End ' || procedureName,rowCt,stepCt,'Done') into rtnCd;
	--commit;
	
    ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		perform cz_end_audit (jobID, 'SUCCESS');
	END IF;

	EXCEPTION
	WHEN OTHERS THEN
	raise notice 'Error % %', SQLSTATE, SQLERRM;
    --Handle errors.
		perform cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		
		--End Proc
		perform cz_end_audit (jobID, 'FAIL');
	
END;
 
$_$;

ALTER FUNCTION tm_cz.i2b2_secure_study(text, bigint)
    OWNER TO tm_cz;

GRANT EXECUTE ON FUNCTION tm_cz.i2b2_secure_study(text, bigint) TO tm_cz;