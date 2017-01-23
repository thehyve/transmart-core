--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZX_WRITE_AUDIT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZX_WRITE_AUDIT" (JOBID IN NUMBER,
	DATABASENAME IN VARCHAR2 ,
	PROCEDURENAME IN VARCHAR2 ,
	STEPDESC IN VARCHAR2 ,
	RECORDSMANIPULATED IN NUMBER,
	STEPNUMBER IN NUMBER,
	STEPSTATUS IN VARCHAR2)
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

  LASTTIME TIMESTAMP;
  v_version_id NUMBER;

BEGIN
  SELECT MAX(JOB_DATE)
    INTO LASTTIME
    FROM CZ_JOB_AUDIT
    WHERE JOB_ID = JOBID;

	INSERT 	INTO CZ_JOB_AUDIT(
		JOB_ID,
		DATABASE_NAME,
 		PROCEDURE_NAME,
 		STEP_DESC,
		RECORDS_MANIPULATED,
		STEP_NUMBER,
		STEP_STATUS,
    JOB_DATE,
    TIME_ELAPSED_SECS
	)
	SELECT
 		JOBID,
		DATABASENAME,
		PROCEDURENAME,
		STEPDESC,
		RECORDSMANIPULATED,
		STEPNUMBER,
		STEPSTATUS,
    SYSTIMESTAMP,
      COALESCE(
      EXTRACT (DAY    FROM (SYSTIMESTAMP - LASTTIME))*24*60*60 +
      EXTRACT (HOUR   FROM (SYSTIMESTAMP - LASTTIME))*60*60 +
      EXTRACT (MINUTE FROM (SYSTIMESTAMP - LASTTIME))*60 +
      EXTRACT (SECOND FROM (SYSTIMESTAMP - LASTTIME))
      ,0)
  FROM DUAL;

  COMMIT;

EXCEPTION
    WHEN OTHERS THEN ROLLBACK;
END;
 
/
 
