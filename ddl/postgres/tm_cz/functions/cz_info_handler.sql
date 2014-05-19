--
-- Name: cz_info_handler(bigint, bigint, bigint, text, text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION cz_info_handler (
	jobId IN numeric,
	messageID IN numeric, 
	messageLine IN numeric, 
	messageProcedure IN character varying , 
	infoMessage IN character varying,
  stepNumber IN character varying
)
 RETURNS VOID AS $body$
DECLARE

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

  databaseName varchar(100);

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
 
$body$
LANGUAGE PLPGSQL;
