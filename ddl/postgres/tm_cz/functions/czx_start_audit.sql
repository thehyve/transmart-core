--
-- Name: czx_start_audit(text, text, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION czx_start_audit (V_JOB_NAME IN character varying DEFAULT NULL ,
  V_DATABASE_NAME IN character varying DEFAULT NULL ,
  O_JOB_ID OUT numeric)
  --AUTHID CURRENT_USER  
 RETURNS numeric AS $body$
DECLARE

  PRAGMA AUTONOMOUS_TRANSACTION;
/*************************************************************************
* Copyright 2008-2012 Janssen Research and Development, LLC.
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

	v_os_user	varchar(200);
	v_job_id	numeric;


BEGIN

   INSERT INTO CZ_JOB_MASTER
     ( START_DATE, 
		ACTIVE, 
		DATABASE_NAME, 
		JOB_NAME, 
		JOB_STATUS )
     VALUES (
		LOCALTIMESTAMP, 
		'Y', 
		V_DATABASE_NAME, 
		V_JOB_NAME, 
		'Running' )

	RETURNING JOB_ID INTO V_JOB_ID;
	
	PERFORM sys_context('USERENV','OS_USER') into v_os_user ;
	
	INSERT INTO cz_job_message
    ( job_id, message_id, message_procedure, info_message )
    VALUES ( v_job_id, 1, 'OS user name',v_os_user );

	
	COMMIT;
	
	o_job_id := v_job_id;
  
EXCEPTION
    WHEN OTHERS THEN ROLLBACK;
END;
 
$body$
LANGUAGE PLPGSQL;
