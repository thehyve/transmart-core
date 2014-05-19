--
-- Name: czx_write_error(bigint, bigint, text, text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION czx_write_error (JOBID IN numeric,
	ERRORNUMBER IN numeric , 
	ERRORMESSAGE IN character varying , 
	ERRORSTACK IN character varying,
  ERRORBACKTRACE IN character varying)
--  AUTHID CURRENT_USER
 RETURNS VOID AS $body$
DECLARE

 PRAGMA AUTONOMOUS_TRANSACTION;
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

	INSERT INTO CZ_JOB_ERROR(
		JOB_ID,
		ERROR_NUMBER,
		ERROR_MESSAGE,
		ERROR_STACK,
    ERROR_BACKTRACE,
		SEQ_ID)
	PERFORM
		JOBID,
		ERRORNUMBER,
		ERRORMESSAGE,
		ERRORSTACK,
    ERRORBACKTRACE,
		MAX(SEQ_ID) 
  FROM 
    CZ_JOB_AUDIT 
  WHERE 
    JOB_ID=JOBID;
  
  COMMIT;
 
EXCEPTION
    WHEN OTHERS THEN ROLLBACK;
END;
 
$body$
LANGUAGE PLPGSQL;
