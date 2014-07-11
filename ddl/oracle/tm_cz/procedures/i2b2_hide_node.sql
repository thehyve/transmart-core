--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_HIDE_NODE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_HIDE_NODE" 
(
  path VARCHAR2
)
AS
BEGIN
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

  if path != ''  and path != '%'
  then 
  
	update i2b2 b
	set c_visualattributes=substr(b.c_visualattributes,1,1) || 'H' || substr(b.c_visualattributes,3,1)
	where c_fullname like path || '%';
	
	delete from concept_counts
	where concept_path like path || '%';
	
	commit;
	
	
/*
      --I2B2
     UPDATE i2b2
      SET c_visualattributes = 'FH'
    WHERE c_visualattributes like 'F%'
      AND C_FULLNAME LIKE PATH || '%';

     UPDATE i2b2
      SET c_visualattributes = 'LH'
    WHERE c_visualattributes like 'L%'
      AND C_FULLNAME LIKE PATH || '%';
    COMMIT;
*/
  END IF;
  
END;
 
 
/
 
