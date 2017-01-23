--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_OMICSOFT_DATA
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_OMICSOFT_DATA" 
(analysis_id IN number
 ,platform_id			in  varchar2
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
	
	v_etl_id					varchar2(200);
	v_bio_assay_analysis_id		number(18,0);
  v_platform_id		varchar2(20);
  v_bio_experiment_id		number(18,0);
  v_bio_assay_platform_id number(18,0);
  v_folder_full_name       varchar2(500);
  v_folder_uid        varchar2(300);
  v_counter           int;
  v_folder_type       varchar2(50);
  v_accession         varchar2(50);
  v_data_cnt          int;
  v_tea_data_cnt      int;
  v_mean_fold_change number(18,4);
  v_stdev_fold_change number(18,4);
	v_sqlText					varchar2(2000);
	v_rowcount					int;
  v_exists            int;

	
	no_analysis_data	exception;
	
	BEGIN	
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := i_job_id;
  rtn_code := 0;

  EXECUTE IMMEDIATE 'alter session set NLS_NUMERIC_CHARACTERS=".,"'; 

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
	czx_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_omicsoft_data',0,stepCt,'Done');
	
  -- set study and analysis variables
  v_data_cnt := 0;
  v_tea_data_cnt := 0;
  v_bio_assay_analysis_id := analysis_id;
  v_platform_id := platform_id;
  
  --select etl_id into v_accession from biomart.bio_assay_analysis where bio_assay_analysis_id = v_bio_assay_analysis_id;
  --select bio_experiment_id  into v_bio_experiment_id from biomart.bio_experiment where accession = v_accession;
  
  select ff.folder_full_name into v_folder_full_name
  from biomart.bio_data_uid bdu 
    join fmapp.fm_folder_association ffa on ffa.object_uid = bdu.unique_id -- analysis folder
    join fmapp.fm_folder ff on ff.folder_id = ffa.folder_id and ff.folder_type = 'ANALYSIS'
    where bdu.bio_data_id = v_bio_assay_analysis_id;
    
    stepCt := 1;
    czx_write_audit(jobId,databaseName,procedureName,'Folder full name = ' || v_folder_full_name ,0,stepCt,'Done');
  
  -- strip trailing backslash
  v_folder_full_name := SUBSTR(v_folder_full_name, 1, length(v_folder_full_name) - 1);
  v_counter := 1; -- start at last backslash
  v_folder_type := 'TEST';
  
  stepCt := 1;
  czx_write_audit(jobId,databaseName,procedureName,'Folder type = ' || v_folder_type ,0,stepCt,'Done');
  czx_write_audit(jobId,databaseName,procedureName,'Platform = ' || v_platform_id ,0,stepCt,'Done');
   
  while v_folder_type != 'STUDY'
  loop
    -- get last folder UID in path and check it's folder_type
    v_folder_uid := SUBSTR(v_folder_full_name, INSTR(v_folder_full_name,'\', -1, v_counter)+1);
    select f.folder_type into v_folder_type 
      from fmapp.fm_folder f
      join fmapp.fm_data_uid fdu on fdu.fm_data_id = f.folder_id
      where fdu.unique_id = v_folder_uid;
    v_folder_full_name := SUBSTR(v_folder_full_name, 1, INSTR(v_folder_full_name,'\', -1, v_counter)-1);
  end loop;
  
  stepCt := 1;
  czx_write_audit(jobId,databaseName,procedureName,'StudyFolderUID =  ' || v_folder_uid ,0,stepCt,'Done');
  
  select be.bio_experiment_id into v_bio_experiment_id
  from BIOMART.bio_experiment be
  join biomart.bio_data_uid bdu on bdu.bio_data_id = be.bio_experiment_id
  join fmapp.fm_folder_association ffa on ffa.object_uid = bdu.unique_id
  join fmapp.fm_data_uid fdu on fdu.fm_data_id = ffa.folder_id
  where fdu.unique_id = v_folder_uid;

  begin
    select bdup.bio_data_id into v_bio_assay_platform_id 
      from fmapp.fm_folder_association ffa
      join fmapp.fm_data_uid fdu on fdu.fm_data_id = ffa.folder_id
      join fmapp.fm_folder ff on ff.folder_id = ffa.folder_id and ff.folder_type = 'ANALYSIS'
      join biomart.bio_data_uid bdu on bdu.unique_id = ffa.object_uid
      join amapp.am_tag_association ata on ata.subject_uid =  fdu.unique_id and ata.object_type = 'BIO_ASSAY_PLATFORM'
      join biomart.bio_data_uid bdup on bdup.unique_id = ata.object_uid
      where bdu.bio_data_id = v_bio_assay_analysis_id
      and rownum = 1; -- how do you select correct platform if analysis has > 1?;
    exception
      when NO_DATA_FOUND then NULL;
  end;
    
  update tm_lz.lt_src_omicsoft_data
  set raw_p_value = null
  where tm_cz.is_number(raw_p_value) = 1;
  
  update tm_lz.lt_src_omicsoft_data
  set adj_p_value = null
  where tm_cz.is_number(adj_p_value) = 1;
  
  update tm_lz.lt_src_omicsoft_data
  set fold_change = null
  where tm_cz.is_number(fold_change) = 1;
  
  select round(avg(fold_change),4) into v_mean_fold_change
  from tm_lz.lt_src_omicsoft_data;
  
  select round(stddev(fold_change),4) into v_stdev_fold_change
  from tm_lz.lt_src_omicsoft_data;

  insert into BIOMART.bio_assay_analysis_data(
  bio_assay_analysis_id
  , bio_experiment_id
  , bio_assay_platform_id
  , feature_group_name
  , adjusted_pvalue
  , raw_pvalue
  , fold_change_ratio
  , preferred_pvalue
  --, bio_assay_feature_group_id
  , tea_normalized_pvalue
  , etl_id
  ,probeset_id)
    select distinct
    lz.bio_assay_analysis_id -- analysis
    , v_bio_experiment_id --study
    , v_bio_assay_platform_id -- platform
    , lz.probe_id -- feature_group
    , lz.adj_p_value
    , lz.raw_p_value
    , lz.fold_change
    , lz.raw_p_value -- NO PREFERRED PVALUE DATA PROVIDED
    --, fg.bio_assay_feature_group_id
    , round(biomart.TEA_NPV_PRECOMPUTE(lz.fold_change, v_mean_fold_change, v_stdev_fold_change),4)
    , etl_id
    , an.probeset_id
    from tm_lz.lt_src_omicsoft_data lz, deapp.de_mrna_annotation an
    where (fold_change >=1.2 or fold_change <=-1.2)
    and (lz.raw_p_value is null or to_number(lz.raw_p_value) <= 0.05) --NO PREFERRED PVALUE DATA PROVIDED
    and an.gpl_id= v_platform_id
    and an.probe_id=lz.probe_id;
    
    v_data_cnt := SQL%ROWCOUNT;
 
  czx_write_audit(jobId,databaseName,procedureName,'Loaded bio_assay_analysis_data with ' || v_data_cnt || ' records' ,0,stepCt,'Done');
	stepCt := stepCt + 1;
  
  if v_data_cnt = 0 then
		raise no_analysis_data;
	end if;
  
  	--	drop indexes
    select count(*) into v_exists 
    from all_indexes
    where table_name='BIO_ASSAY_ANALYSIS_DATA_TEA'
    and owner = 'BIOMART'
    and index_name not like 'PK_%' and index_name not like '%_PK';
    
    if v_exists > 0 then
    begin
      declare cursor c1 is select index_name from all_indexes
      where table_name='BIO_ASSAY_ANALYSIS_DATA_TEA'
      and owner = 'BIOMART'
      and index_name not like 'PK_%' and index_name not like '%_PK';
      
      v_cmd  varchar2(200);
      v_index_name varchar2(200);
  
      begin
        open c1;
        loop
          fetch c1 into v_index_name;
          if c1%notfound then exit;
          end if;
          v_cmd := 'drop index biomart.' || v_index_name;
          execute immediate v_cmd;
        end loop;
      end;
      
    end;
    end if;
  
  insert into BIOMART.bio_assay_analysis_data_tea(
   bio_asy_analysis_data_id
  , bio_assay_analysis_id
  , bio_experiment_id
  , bio_assay_platform_id
  , feature_group_name
  , etl_id
  , adjusted_pvalue
  , raw_pvalue
  , fold_change_ratio
  , preferred_pvalue
  --, bio_assay_feature_group_id
  , tea_normalized_pvalue
  , probeset_id)
    select distinct
      baad.bio_asy_analysis_data_id
    , baad.bio_assay_analysis_id -- analysis
    , baad.bio_experiment_id --study
    , baad.bio_assay_platform_id
   -- , baad.feature_group_name 
    , baad.etl_id
    , baad.adjusted_pvalue
    , baad.raw_pvalue
    , baad.fold_change_ratio
    , baad.preferred_pvalue
    , baad.bio_assay_feature_group_id
    , baad.tea_normalized_pvalue
    , baad.probeset_id
    from biomart.bio_assay_analysis_data baad
    join tm_lz.lt_src_omicsoft_data lz on lz.etl_id = baad.etl_id
    where --baad.bio_assay_analysis_id = v_bio_assay_analysis_id
    baad.tea_normalized_pvalue <= 0.05;
    
  v_tea_data_cnt := SQL%ROWCOUNT;
  
  czx_write_audit(jobId,databaseName,procedureName,'Loaded bio_assay_analysis_data_tea with ' || v_tea_data_cnt || ' records' ,0,stepCt,'Done');
	stepCt := stepCt + 1;
  
  update biomart.bio_assay_analysis
  set data_count = v_data_cnt, tea_data_count = v_tea_data_cnt
  where bio_assay_analysis_id = v_bio_assay_analysis_id;
    
	--	recreate indexes

		execute immediate('create index biomart.baad_idx_tea_probe_name on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (FEATURE_GROUP_NAME, BIO_ASY_ANALYSIS_DATA_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_probe_name',0,stepCt,'Done');
		
    execute immediate('create index biomart.baad_idx_tea_probe_id on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_ASSAY_FEATURE_GROUP_ID, BIO_ASY_ANALYSIS_DATA_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_probe_id',0,stepCt,'Done');
		
    execute immediate('create index biomart.baad_idx_tea_experiment_type on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_EXPERIMENT_TYPE, BIO_ASY_ANALYSIS_DATA_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_experiment_type',0,stepCt,'Done');
    
    execute immediate('create index biomart.baad_idx_tea_exp_analysis on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_EXPERIMENT_ID, BIO_ASSAY_ANALYSIS_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_exp_analysis',0,stepCt,'Done');
		
    execute immediate('create index biomart.baad_idx_tea_exp_analysis1 on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_EXPERIMENT_ID, BIO_ASSAY_ANALYSIS_ID, BIO_ASY_ANALYSIS_DATA_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_exp_analysis1',0,stepCt,'Done');
		
    execute immediate('create index biomart.baad_idx_tea_analysis on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_ASSAY_ANALYSIS_ID, BIO_ASY_ANALYSIS_DATA_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_analysis',0,stepCt,'Done');
    
    execute immediate('create index biomart.baad_idx_tea_probe_analysis on biomart.BIO_ASSAY_ANALYSIS_DATA_TEA (BIO_ASSAY_FEATURE_GROUP_ID, BIO_ASSAY_ANALYSIS_ID) TABLESPACE INDX NOLOGGING PARALLEL 8');
		stepCt := stepCt + 1;
		czx_write_audit(jobId,databaseName,procedureName,'Created index baad_idx_tea_probe_analysis',0,stepCt,'Done');

  commit;
	
	czx_write_audit(jobId,databaseName,procedureName,'End i2b2_load_omicsoft_data',0,stepCt,'Done');
	stepCt := stepCt + 1;
	
	czx_end_audit(jobId, 'Success');
	
	exception
	when no_analysis_data then
		czx_write_audit(jobId, databaseName, procedureName, 'No analysis data to load - terminating normally',0,stepCt,'Done');
		czx_end_audit(jobId, 'Success');
	when others then
    --Handle errors.
		czx_error_handler (jobID, procedureName);
    --End Proc
		czx_end_audit (jobID, 'FAIL');
    rtn_code := 16;
	
END;
/
 
