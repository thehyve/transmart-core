--
-- Type: FUNCTION; Owner: BIOMART; Name: TEA_NPV_PRECOMPUTE
--
  CREATE OR REPLACE FUNCTION "BIOMART"."TEA_NPV_PRECOMPUTE" ( 
  foldChg IN NUMBER, 
  mu IN NUMBER, 
  sigma IN NUMBER
) RETURN NUMBER AS

 -------------------------------------------------------------------------------
  -- used for gene signature TEA algorithm which precomputes normalized p-values
  -- based on fold change ratio from analysis_data records
  -- JWS@20090601 - First rev.
  -- param foldChg: input is fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------
  npv number;
  outlier_cutoff number:=1.0e-5;
  
BEGIN
  -- normalized p-value 
  npv:= 1.0 - cum_normal_dist(abs(foldChg),mu,sigma);
  
  -- cap outliers to minimum value
  IF npv<outlier_cutoff THEN npv:= outlier_cutoff; END IF;
  
  RETURN npv;
  
END TEA_NPV_PRECOMPUTE;

 
 
 
 
 
 
/
 
