--
-- Name: czx_write_audit(numeric, character varying, character varying, character varying, numeric, numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION czx_write_audit (JOBID IN numeric,
	DATABASENAME IN character varying,
	PROCEDURENAME IN character varying,
	STEPDESC IN character varying,
	RECORDSMANIPULATED IN numeric,
	STEPNUMBER IN numeric,
	STEPSTATUS IN character varying)
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

  LASTTIME timestamp;
  v_version_id numeric;


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
	PERFORM
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
  ;

  COMMIT;

EXCEPTION
    WHEN OTHERS THEN ROLLBACK;
END;
 
$body$
LANGUAGE PLPGSQL;
