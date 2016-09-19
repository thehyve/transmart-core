--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_DELETE_1_NODE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_DELETE_1_NODE" 
(
  path VARCHAR2
 ,currentJobID NUMBER := null
) AUTHID CURRENT_USER
AS
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

  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);

begin
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
    czx_start_audit (procedureName, databaseName, jobID);
  END IF;
    	
  stepCt := 0;  
  if coalesce(path,'') = ''  or path = '%'
	then 
		czx_write_audit(jobId,databaseName,procedureName,'Path missing or invalid',0,stepCt,'Done'); 
  else
    --I2B2
    DELETE 
      FROM OBSERVATION_FACT 
    WHERE 
      concept_cd IN (SELECT C_BASECODE FROM I2B2 WHERE C_FULLNAME = PATH);
	stepCt := stepCt + 1;
	czx_write_audit(jobId,databaseName,procedureName,'Delete data for node from I2B2DEMODATA observation_fact',SQL%ROWCOUNT,stepCt,'Done');
    COMMIT;

      --CONCEPT DIMENSION
    DELETE 
      FROM CONCEPT_DIMENSION
    WHERE 
      CONCEPT_PATH = path;
	stepCt := stepCt + 1;
	czx_write_audit(jobId,databaseName,procedureName,'Delete data for node from I2B2DEMODATA concept_dimension',SQL%ROWCOUNT,stepCt,'Done');
    COMMIT;
    
      --I2B2
      DELETE
        FROM i2b2
      WHERE 
        C_FULLNAME = PATH;
	stepCt := stepCt + 1;
	czx_write_audit(jobId,databaseName,procedureName,'Delete data for node from I2B2METADATA i2b2',SQL%ROWCOUNT,stepCt,'Done');
    COMMIT;

  --i2b2_secure
      DELETE
        FROM i2b2_secure
      WHERE 
        C_FULLNAME = PATH;
	stepCt := stepCt + 1;
	czx_write_audit(jobId,databaseName,procedureName,'Delete data for node from I2B2METADATA i2b2_secure',SQL%ROWCOUNT,stepCt,'Done');
    COMMIT;

  --concept_counts
      DELETE
        FROM concept_counts
      WHERE 
        concept_path = PATH;
	stepCt := stepCt + 1;
	czx_write_audit(jobId,databaseName,procedureName,'Delete data for node from I2B2DEMODATA concept_counts',SQL%ROWCOUNT,stepCt,'Done');
    COMMIT;

  END IF;
  
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    czx_end_audit (jobID, 'SUCCESS');
  END IF;

  EXCEPTION
  WHEN OTHERS THEN
    --Handle errors.
    czx_error_handler (jobID, procedureName);
    --End Proc
    czx_end_audit (jobID, 'FAIL');    
END;

 
/
 
