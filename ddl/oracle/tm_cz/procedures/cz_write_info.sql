--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_WRITE_INFO
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_WRITE_INFO" 
(
	jobId IN NUMBER,
	messageID IN NUMBER , 
	messageLine IN NUMBER, 
	messageProcedure IN VARCHAR2 , 
	infoMessage IN VARCHAR2
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
BEGIN

	insert into cz_job_message
    (
      job_id,
      message_id,
      message_line,
      message_procedure,
      info_message,
      seq_id
    )
	select
      jobID,
      messageID,
      messageLine,
      messageProcedure,
      infoMessage,
      max(seq_id)
  from
    cz_job_audit
  where
    job_id = jobID;
  
  COMMIT;

END;
/
 
