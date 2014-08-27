--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_SAMPLE_CATEGORIES
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_SAMPLE_CATEGORIES" 

(
  currentJobID NUMBER := null
)
AS


	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0);
	stepCt number(18,0);

	--	JEA@20110916	New
	--	JEA@20120209	Remove insert of sample to patient_dimension

	--
	-- Copyright © 2011 Recombinant Data Corp.
	--

BEGIN
    
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
  
	--	delete any data for study in sample_categories_extrnl from lz_src_sample_categories
	
	delete from lz_src_sample_categories
	where trial_cd in (select distinct trial_cd from lt_src_sample_categories);
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Deleted existing study data in lz_src_sample_categories',SQL%ROWCOUNT,stepCt,'Done');
	commit;	

/*	
	--	create records in patient_dimension for samples if they do not exist
	--	format of sourcesystem_cd:  trial:S:[site:]subject_cd:sample_cd
	--	if no sample_cd specified, then the patient_num of the trial:[site]:subject_cd should have already been created
	
	insert into patient_dimension
    ( patient_num,
      sex_cd,
      age_in_years_num,
      race_cd,
      update_date,
      download_date,
      import_date,
      sourcesystem_cd
    )
    select seq_patient_num.nextval,
		   null as sex_cd,
		   0 as age_in_years_num,
		   null as race_cd,
		   sysdate,
		   sysdate,
		   sysdate,
		   regexp_replace(s.trial_cd || ':S:' || s.site_cd || ':' || s.subject_cd || ':' || s.sample_cd,
						  '(::){1,}', ':')
	from (select distinct trial_cd
	             ,site_cd
				 ,subject_cd
				 ,sample_cd
		  from sample_categories_extrnl s
		  where s.sample_cd is not null
			and not exists
				(select 1 from patient_dimension x
				 where x.sourcesystem_cd = 
					   regexp_replace(s.trial_cd || ':S:' || s.site_cd || ':' || s.subject_cd || ':' || s.sample_cd,
					 '(::){1,}', ':'))
		  ) s;
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Added new sample_cds for study in I2B2DEMODATA patient_dimension',SQL%ROWCOUNT,stepCt,'Done');
	commit;
*/

	--	Load data into lz_src_sample_categories table, joins to make sure study/trial exists and there's an entry in the patient_dimension
	
	insert into lz_src_sample_categories
	(trial_cd
	,site_cd
	,subject_cd
	,sample_cd
	,category_cd
	,category_value
	)
	select distinct s.trial_cd
		  ,s.site_cd
		  ,s.subject_cd
		  ,s.sample_cd
		  ,replace(s.category_cd,'"',null)
		  ,replace(s.category_value,'"',null)
	from lt_src_sample_categories s
	where replace(s.category_cd,'"',null) is not null
	  and replace(s.category_value,'"',null) is not null
	  and s.trial_cd in (select distinct x.sourcesystem_cd from i2b2 x)
	;
		  
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Inserted sample data in lz_src_sample_categories',SQL%ROWCOUNT,stepCt,'Done');
	commit;

	if newjobflag = 1
	then
		cz_end_audit (jobID, 'SUCCESS');
	end if;

	exception
	when others then
		--Handle errors.
		cz_error_handler (jobID, procedureName);
		
		--End Proc
		cz_end_audit (jobID, 'FAIL');
	
END;

 
/
 
