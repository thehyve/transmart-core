--
-- Name: fract_error_dist(numeric); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION fract_error_dist(norminput numeric) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE


 -------------------------------------------------------------------------------
  -- implementation of fractional error distribution
  -- JWS@20090601 - First rev.
  -------------------------------------------------------------------------------
  -- temp var
  t1 decimal:= 1.0 / (1.0 + 0.5 * ABS(normInput));
  
  -- exponent input to next equation
  exponent_input decimal:= -normInput*normInput - 1.26551223 + 
                           t1*(1.00002368 + t1*(0.37409196 + t1*(0.09678418 + t1*(-0.18628806 + t1*(0.27886807 + t1*(-1.13520398 + t1*(1.48851587 + t1*(-0.82215223 + t1*(0.17087277)))))))));
  -- Horner's method
  ans decimal:= 1 - t1 * EXP(exponent_input);

  fractError decimal;


BEGIN
  -- handle sign
  IF normInput>0.0 THEN fractError:= ans; ELSE fractError:= -ans; END IF;
  return fractError;

END;
$$;

