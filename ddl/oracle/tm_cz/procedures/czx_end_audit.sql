--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZX_END_AUDIT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZX_END_AUDIT" (V_JOB_ID IN NUMBER DEFAULT NULL ,
  V_JOB_STATUS IN VARCHAR2 DEFAULT 'Success')
  AUTHID CURRENT_USER
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

 PRAGMA AUTONOMOUS_TRANSACTION;
 
  ENDDATE TIMESTAMP;

BEGIN
  DBMS_OUTPUT.PUT_LINE('Job ID = ' || V_JOB_ID || ',' || V_JOB_STATUS);
  
  ENDDATE := SYSTIMESTAMP;
  
	UPDATE CZ_JOB_MASTER
		SET 
			ACTIVE='N',
			END_DATE = ENDDATE,
      TIME_ELAPSED_SECS = 
      EXTRACT (DAY    FROM (ENDDATE - START_DATE))*24*60*60 + 
      EXTRACT (HOUR   FROM (ENDDATE - START_DATE))*60*60 + 
      EXTRACT (MINUTE FROM (ENDDATE - START_DATE))*60 + 
      EXTRACT (SECOND FROM (ENDDATE - START_DATE)),
			JOB_STATUS = V_JOB_STATUS
		WHERE ACTIVE='Y' 
		AND JOB_ID=V_JOB_ID;

COMMIT;

	IF V_JOB_STATUS = 'FAIL'
	THEN
		DBMS_OUTPUT.PUT_LINE('Job Failed - See cz_job_error for details');
	END IF;
  
--EXCEPTION
--	WHEN OTHERS THEN 
--	DBMS_OUTPUT.PUT_LINE('ERROR HERE!');
--    ROLLBACK;  
END;
/
 
