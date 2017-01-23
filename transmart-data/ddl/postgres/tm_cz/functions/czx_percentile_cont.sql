--
-- Name: czx_percentile_cont(real[], real); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czx_percentile_cont(myarray real[], percentile real) RETURNS real
    LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER
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
DECLARE   
  ary_cnt INTEGER;   
  row_num real;   
  crn real;   
  frn real;   
  calc_result real;   
  new_array real[];   
BEGIN   
  ary_cnt = array_length(myarray,1);   
  row_num = 1 + ( percentile * ( ary_cnt - 1 ));   
  new_array = tm_cz.czx_array_sort(myarray);   
     
  crn = ceiling(row_num);   
  frn = floor(row_num);   
         
      if crn = frn and frn = row_num then   
    calc_result = new_array[row_num];   
  else   
    calc_result = (crn - row_num) * new_array[frn]   
            + (row_num - frn) * new_array[crn];   
  end if;   
     
  RETURN calc_result;   
END;   
$$;

