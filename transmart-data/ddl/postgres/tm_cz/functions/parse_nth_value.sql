--
-- Name: parse_nth_value(character varying, numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION parse_nth_value(pvalue character varying, location numeric, delimiter character varying) RETURNS character varying
    LANGUAGE plpgsql
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
Declare
   v_posA integer;
   v_posB integer;
   iLoc	  integer;

begin
   iLoc := location;
   if location = 1 then
      v_posA := 1; -- Start at the beginning
   else
	  select tm_cz.instr(pValue,delimiter, 1, iLoc-1) into v_posA;
 --     v_posA := tm_cz.instr(pValue,delimiter, 1, location - 1); 
      if v_posA = 0 then
         return null; --No values left.
      end if;
      v_posA := v_posA + length(delimiter);
   end if;

   select tm_cz.instr (pValue, delimiter, 1, iLoc) into v_posB;
   --v_posB := tm_cz.instr (pValue, delimiter, 1, location);
   if v_posB = 0 then -- Use the end of the file
      return substr (pValue, v_posA);
   end if;
   
   return substr (pValue, v_posA, v_posB - v_posA);

end;
$$;

