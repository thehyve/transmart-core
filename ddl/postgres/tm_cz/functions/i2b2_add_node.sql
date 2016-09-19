--
-- Name: i2b2_add_node(character varying, character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_add_node(trialid character varying, path character varying, path_name character varying, currentjobid numeric) RETURNS integer
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

  root_node		varchar(2000);
  root_level	integer;
  rtnCd			integer;
  
	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
  
BEGIN
    
	stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_ADD_NODE';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobId;
	END IF;
  
	select tm_cz.parse_nth_value(path, 2, '\') into root_node;
	
	select c_hlevel into root_level
	from i2b2metadata.table_access
	where c_name = root_node;
  
	if path = ''  or path = '%' or path_name = ''
	then 
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Path or Path name missing, no action taken',0,stepCt,'Done') into rtnCd;
		return 1;
	end if;
	

	--Delete existing data.

	DELETE FROM i2b2demodata.OBSERVATION_FACT 
	WHERE concept_cd IN (SELECT C_BASECODE FROM i2b2metadata.I2B2 WHERE C_FULLNAME = PATH);
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Deleted any concepts for path from I2B2DEMODATA observation_fact',rowCt,stepCt,'Done') into rtnCd;

	--CONCEPT DIMENSION
	DELETE FROM i2b2demodata.CONCEPT_DIMENSION
	WHERE CONCEPT_PATH = path;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Deleted any concepts for path from I2B2DEMODATA concept_dimension',rowCt,stepCt,'Done') into rtnCd;
    
	--I2B2
	DELETE FROM i2b2metadata.i2b2
	WHERE C_FULLNAME = PATH;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Deleted path from I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;

	--	Insert new node
	
	--CONCEPT DIMENSION
	INSERT INTO i2b2demodata.CONCEPT_DIMENSION
	(CONCEPT_CD, CONCEPT_PATH, NAME_CHAR,  UPDATE_DATE,  DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD)
	VALUES
	(cast(nextval('i2b2demodata.concept_id') as varchar),
	path,
	path_name,
	current_timestamp,
	current_timestamp,
	current_timestamp,
	TrialID
	);
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted concept for path into I2B2DEMODATA concept_dimension',rowCt,stepCt,'Done') into rtnCD;

	--I2B2
	INSERT INTO i2b2metadata.I2B2
	(c_hlevel, C_FULLNAME, C_NAME, C_VISUALATTRIBUTES, c_synonym_cd, C_FACTTABLECOLUMN, C_TABLENAME, C_COLUMNNAME,
	C_DIMCODE, C_TOOLTIP, UPDATE_DATE, DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD, c_basecode, C_OPERATOR, c_columndatatype, c_comment,
	m_applied_path)
	SELECT 
	(length(concept_path) - coalesce(length(replace(concept_path, '\','')),0)) / length('\') - 2 + root_level,
	CONCEPT_PATH,
	NAME_CHAR,
	'FA',
	'N',
	'CONCEPT_CD',
	'CONCEPT_DIMENSION',
	'CONCEPT_PATH',
	CONCEPT_PATH,
	CONCEPT_PATH,
	current_timestamp,
	current_timestamp,
	current_timestamp,
	SOURCESYSTEM_CD,
	CONCEPT_CD,
	'LIKE',
	'T',
	case when TrialID is null then null else 'trial:' || TrialID end,
	'@'
	FROM i2b2demodata.CONCEPT_DIMENSION
	WHERE 
	CONCEPT_PATH = path;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted path into I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;
		
      ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCD;
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

