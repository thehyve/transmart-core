--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_OMICSOFT_ANNOT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_OMICSOFT_ANNOT" 
(analysis_id IN number
,i_job_id		number	:= null
,rtn_code		OUT	NUMBER
)
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research , LLC.
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
	newJobFlag 	int;
	databaseName 	VARCHAR2(100);
	procedureName VARCHAR2(100);
	jobID 		number(18,0);
	stepCt 		number(18,0);

	v_probe_id		    varchar2(200);
  v_gene_symbol 		varchar2(1000);
  v_gene_id          number(18,0);
  v_organism         varchar2(200);
  
  v_row_count     int;
	
	no_new_annotations	exception;
	
	BEGIN	
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := i_job_id;
  rtn_code := 0;
  v_row_count := 0;


	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		czx_start_audit (procedureName, databaseName, jobID);
	END IF;
    	
	stepCt := 1;	
	czx_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_omicsoft_annot',0,stepCt,'Done');

  -- load any missing probe_ids into bio_assay_feature_group
  insert into BIOMART.bio_assay_feature_group(
    feature_group_name
  , feature_group_type)
    select distinct
    probe_id
  , 'PROBESET'
    from TM_LZ.lt_src_omicsoft_annot lz
    where probe_id not in (select feature_group_name from BIOMART.bio_assay_feature_group);
    
    v_row_count := SQL%ROWCOUNT;
 
  czx_write_audit(jobId,databaseName,procedureName,'Loaded bio_assay_featurwe_group with ' || v_row_count || ' records' ,0,stepCt,'Done');
	stepCt := stepCt + 1;
  
  if v_row_count = 0 then
		raise no_new_annotations;
	end if;
  
  -- need to get platform from analysis
  insert into BIOMART.bio_assay_data_annotation(
   bio_assay_feature_group_id
  , bio_marker_id
  , data_table)
    select distinct
      bafg.bio_assay_feature_group_id
    , bm.bio_marker_id -- analysis
    , 'BAAD'
    from biomart.bio_assay_feature_group bafg
    join TM_LZ.lt_src_omicsoft_annot lz on lz.probe_id = bafg.feature_group_name
    join biomart.bio_marker bm on bm.bio_marker_name = lz.gene_symbol and upper(bm.organism) = upper(lz.organism)
    where not exists (select 1 from biomart.bio_assay_data_annotation 
                      where bio_assay_feature_group_id = bafg.bio_assay_feature_group_id
                      and bio_marker_id = bm.bio_marker_id);
    
  v_row_count := SQL%ROWCOUNT;
  
  czx_write_audit(jobId,databaseName,procedureName,'Loaded bio_assay_data_annotation with ' || v_row_count || ' records' ,0,stepCt,'Done');
	stepCt := stepCt + 1;
	
	czx_write_audit(jobId,databaseName,procedureName,'End i2b2_load_omicsoft_annot',0,stepCt,'Done');
	stepCt := stepCt + 1;
	
	czx_end_audit(jobId, 'Success');
	
	exception
	when no_new_annotations then
		czx_write_audit(jobId, databaseName, procedureName, 'No new feature groups to load - terminating normally',0,stepCt,'Done');
		czx_end_audit(jobId, 'Success');
	when others then
    --Handle errors.
		czx_error_handler (jobID, procedureName);
    --End Proc
		czx_end_audit (jobID, 'FAIL');
    rtn_code := 16;
	
END;
/
 
