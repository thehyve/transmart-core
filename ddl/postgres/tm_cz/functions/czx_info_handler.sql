--
-- Name: czx_info_handler(numeric, numeric, numeric, character varying, character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czx_info_handler(jobid numeric, messageid numeric, messageline numeric, messageprocedure character varying, infomessage character varying, stepnumber character varying) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
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

declare

  databaseName VARCHAR(100);
  rtnCd			numeric;
BEGIN

	select 
    database_name INTO databasename
  from 
    tm_cz.cz_job_master 
	where 
    job_id=jobID;
    
  select tm_cz.czx_write_audit( jobID, databaseName, messageProcedure, 'Step contains more details', 0, stepNumber, 'Information' ) into rtnCd;

  select tm_cz.czx_write_info(jobID, messageID, messageLine, messageProcedure, infoMessage ) into rtnCd;
  return rtnCd;
  
END;
$$;

