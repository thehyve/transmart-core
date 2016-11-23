--
-- Type: FUNCTION; Owner: BIOMART; Name: CUM_NORMAL_DIST
--
  CREATE OR REPLACE FUNCTION "BIOMART"."CUM_NORMAL_DIST" ( 
  foldChg IN NUMBER, 
  mu IN NUMBER, 
  sigma IN NUMBER
) RETURN NUMBER AS
 -------------------------------------------------------------------------------
  -- implementation of a cumalative normal distribution
  -- JWS@20090601 - First rev.
  -- param foldChg: fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------

  -- temporary vars  
  t1 NUMBER;
  
  -- fractional error dist input
  fract_error_input NUMBER;
  
  -- return result (i.e. Prob [X<=x])
  ans NUMBER;  

BEGIN
  t1:= (foldChg-mu)/sigma;  
  fract_error_input:= t1/SQRT(2);
  ans:= 0.5 * (1.0 + fract_error_dist(fract_error_input));
  return ans; 
END CUM_NORMAL_DIST;

 
 
 
 
 
 
 
/
 
