--
-- Name: cum_normal_dist(numeric, numeric, numeric); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION cum_normal_dist(foldchg numeric, mu numeric, sigma numeric) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE

 -------------------------------------------------------------------------------
  -- implementation of a cumalative normal distribution
  -- JWS@20090601 - First rev.
  -- param foldChg: fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------

  -- temporary vars  
  t1 decimal;
  
  -- fractional error dist input
  fract_error_input decimal;
  
  -- return result (i.e. Prob [X<=x])
  ans decimal;  


BEGIN
  t1:= (foldChg-mu)/sigma;  
  fract_error_input:= t1/SQRT(2);
  ans:= 0.5 * (1.0 + biomart.fract_error_dist(fract_error_input));
  return ans; 
END;
$$;

