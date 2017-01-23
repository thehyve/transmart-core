--
-- Name: cum_normal_dist(double precision, double precision, double precision); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION cum_normal_dist(foldchg double precision, mu double precision, sigma double precision) RETURNS double precision
    LANGUAGE plpgsql IMMUTABLE
    AS $$
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

 -------------------------------------------------------------------------------
  -- param foldChg: fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------

  -- temporary vars
  t1 DOUBLE PRECISION;

  -- fractional error dist input
  fract_error_input DOUBLE PRECISION;

  -- return result (i.e. Prob [X<=x])
  ans DOUBLE PRECISION;

BEGIN
  SELECT (foldChg-mu)/sigma INTO t1;
  SELECT t1/SQRT(2) INTO fract_error_input;
  SELECT 0.5 * (1.0 + tm_cz.fract_error_dist(fract_error_input)) INTO ans;
  return ans;
END
$$;

