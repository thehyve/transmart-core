--
-- Name: tea_npv_precompute(numeric, numeric, numeric); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tea_npv_precompute(foldchg numeric, mu numeric, sigma numeric) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE


 -------------------------------------------------------------------------------
  -- used for gene signature TEA algorithm which precomputes normalized p-values
  -- based ON biomart.fold change ratio from analysis_data records
  -- JWS@20090601 - First rev.
  -- param foldChg: input is fold change ratio from from analysis_data table
  -- param mu: mean of all analsyis_data records for a given analysis
  -- param sigma: std dev of all analsyis_data records for a given analysis
  -------------------------------------------------------------------------------
  npv decimal;
  outlier_cutoff decimal:=1.0e-5;
  

BEGIN
  -- normalized p-value 
  npv:= 1.0 - cum_normal_dist(abs(foldChg),mu,sigma);
  
  -- cap outliers to minimum value
  IF npv<outlier_cutoff THEN npv:= outlier_cutoff; END IF;
  
  RETURN npv;
  
END;
$$;

