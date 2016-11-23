--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_END_AUDIT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_END_AUDIT" 
(
  jobID NUMBER, 
  jobStatus VARCHAR2
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
  endDate timestamp;

BEGIN
  
  endDate := systimestamp;
  
	update cz_job_master
		set 
			active='N',
			end_date = endDate,
      time_elapsed_secs = 
      EXTRACT (DAY    FROM (endDate - START_DATE))*24*60*60 + 
      EXTRACT (HOUR   FROM (endDate - START_DATE))*60*60 + 
      EXTRACT (MINUTE FROM (endDate - START_DATE))*60 + 
      EXTRACT (SECOND FROM (endDate - START_DATE)),
			job_status = jobStatus		
		where active='Y' 
		and job_id=jobID;

END;
/
 
