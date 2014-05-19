--
-- Name: i2b2_hide_node(characrter varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION i2b2_hide_node (
  path character varying
)
 RETURNS VOID AS $body$
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
	set c_visualattributes=substring(b.c_visualattributes from 1 for 1) || 'H' || substring(b.c_visualattributes from 3 for 1)
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
 
$body$
LANGUAGE PLPGSQL;
