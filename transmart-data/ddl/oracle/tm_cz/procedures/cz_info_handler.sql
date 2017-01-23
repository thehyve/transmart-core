--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_INFO_HANDLER
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_INFO_HANDLER" 
(
	jobId IN NUMBER,
	messageID IN NUMBER , 
	messageLine IN NUMBER, 
	messageProcedure IN VARCHAR2 , 
	infoMessage IN VARCHAR2,
  stepNumber IN VARCHAR2
)
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

  databaseName VARCHAR2(100);
BEGIN

	select 
    database_name INTO databasename
  from 
    cz_job_master 
	where 
    job_id=jobID;
    
  cz_write_audit( jobID, databaseName, messageProcedure, 'Step contains more details', 0, stepNumber, 'Information' );

  cz_write_info(jobID, messageID, messageLine, messageProcedure, infoMessage );
  
END;
/
 
