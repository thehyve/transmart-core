--
-- Name: tea_npv_precompute(double precision, double precision, double precision); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tea_npv_precompute(foldchg double precision, mu double precision, sigma double precision) RETURNS double precision
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
  -- param foldChg: input is fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------
  npv double precision;
  outlier_cutoff double precision:=1.0e-5;

BEGIN
  -- normalized p-value
  SELECT 1.0 - tm_cz.cum_normal_dist(abs(foldChg),mu,sigma) INTO npv;

  -- cap outliers to minimum value
  IF npv<outlier_cutoff THEN npv:= outlier_cutoff; END IF;

  RETURN npv;

END;
$$;

