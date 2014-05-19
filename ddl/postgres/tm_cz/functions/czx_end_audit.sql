--
-- Name: czx_end_audit(bigint, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION czx_end_audit (V_JOB_ID IN numeric DEFAULT NULL ,
  V_JOB_STATUS IN character varying DEFAULT 'Success'::character varying)
--  AUTHID CURRENT_USER
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

 PRAGMA AUTONOMOUS_TRANSACTION;
 
  ENDDATE timestamp;


BEGIN
  RAISE NOTICE '%%%%', 'Job ID = ' ,  V_JOB_ID ,  ',' ,  V_JOB_STATUS;
  
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
		RAISE NOTICE 'Job Failed - See cz_job_error for details';
	END IF;
  
--EXCEPTION
--	WHEN OTHERS THEN 
--	DBMS_OUTPUT.PUT_LINE('ERROR HERE!');
--    ROLLBACK;  
END;
 
$body$
LANGUAGE PLPGSQL;
