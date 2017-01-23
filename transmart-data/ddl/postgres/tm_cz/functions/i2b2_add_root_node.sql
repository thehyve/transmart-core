--
-- Name: i2b2_add_root_node(character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_add_root_node(root_node character varying, currentjobid numeric) RETURNS integer
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
DECLARE	
	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);

	rootNode		varchar(200);
	rootPath		varchar(200);
	errorNumber		character varying;
	errorMessage	character varying;
	
	rtnCd			integer;
	
Begin
	rootNode := root_node;
	rootPath := '\' || rootNode || '\';

    stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_ADD_ROOT_NODE';
	

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobId;
	END IF;
	
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Start ' || procedureName,0,stepCt,'Done') into rtnCd;
	
	begin
	
	insert into i2b2metadata.table_access
	(c_table_cd
	,c_table_name
	,c_protected_access
	,c_hlevel
	,c_fullname
	,c_name
	,c_synonym_cd
	,c_visualattributes
	,c_totalnum
	,c_basecode
	,c_metadataxml
	,c_facttablecolumn
	,c_dimtablename
	,c_columnname
	,c_columndatatype
	,c_operator
	,c_dimcode
	,c_comment
	,c_tooltip
	,c_entry_date
	,c_change_date
	,c_status_cd
	,valuetype_cd
	)
	select rootNode as c_table_cd
		  ,'i2b2' as c_table_name
		  ,'N' as protected_access
		  ,0 as c_hlevel
		  ,rootPath as c_fullname
		  ,rootNode as c_name
		  ,'N' as c_synonym_cd
		  ,'CA' as c_visualattributes
		  ,null as c_totalnum
		  ,null as c_basecode
		  ,null as c_metadataxml
		  ,'concept_cd' as c_facttablecolumn
		  ,'concept_dimension' as c_dimtablename
		  ,'concept_path' as c_columnname
		  ,'T' as c_columndatatype
		  ,'LIKE' as c_operator
		  ,rootPath as c_dimcode
		  ,null as c_comment
		  ,rootPath as c_tooltip
		  ,current_timestamp as c_entry_date
		  ,null as c_change_date
		  ,null as c_status_cd
		  ,null as valuetype_cd
	where not exists
		(select 1 from i2b2metadata.table_access x
		 where x.c_table_cd = rootNode);
	get diagnostics rowCt := ROW_COUNT;
	
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert to table_access',rowCt,stepCt,'Done') into rtnCd;	

	--	insert root_node into i2b2
	
	insert into i2b2metadata.i2b2
	(c_hlevel
	,c_fullname
	,c_name
	,c_synonym_cd
	,c_visualattributes
	,c_totalnum
	,c_basecode
	,c_metadataxml
	,c_facttablecolumn
	,c_tablename
	,c_columnname
	,c_columndatatype
	,c_operator
	,c_dimcode
	,c_comment
	,c_tooltip
	,m_applied_path
	,update_date
	,download_date
	,import_date
	,sourcesystem_cd
	,valuetype_cd
	,m_exclusion_cd
	,c_path
	,c_symbol
	--,i2b2_id
	)
	select 0 as c_hlevel
		  ,rootPath as c_fullname
		  ,rootNode as c_name
		  ,'N' as c_synonym_cd
		  ,'CA' as c_visualattributes
		  ,null as c_totalnum
		  ,null as c_basecode
		  ,null as c_metadataxml
		  ,'concept_cd' as c_facttablecolumn
		  ,'concept_dimension' as c_tablename
		  ,'concept_path' as c_columnname
		  ,'T' as c_columndatatype
		  ,'LIKE' as c_operator
		  ,rootPath as c_dimcode
		  ,null as c_comment
		  ,rootPath as c_tooltip
		  ,'@' as m_applied_path
		  ,current_timestamp as update_date
		  ,null as download_date
		  ,current_timestamp as import_date
		  ,null as sourcesystem_cd
		  ,null as valuetype_cd
		  ,null as m_exclusion_cd
		  ,null as c_path
		  ,null as c_symbol
		  --	add trigger on i2b2 insert
		  --,nextval('i2b2metadata.i2b2_id_seq')
		  --,I2B2_ID_SEQ.nextval as i2b2_id
	where not exists
		 (select 1 from i2b2metadata.i2b2 x
		  where x.c_name = rootNode);
	get diagnostics rowCt := ROW_COUNT;
		  
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert root_node ' || rootNode || ' to i2b2',rowCt,stepCt,'Done') into rtnCd;
    
	end;
			
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done') into rtnCD;
	
	--Cleanup OVERALL JOB if this proc is being run standalone
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

