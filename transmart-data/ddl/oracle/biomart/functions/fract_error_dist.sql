--
-- Type: FUNCTION; Owner: BIOMART; Name: FRACT_ERROR_DIST
--
  CREATE OR REPLACE FUNCTION "BIOMART"."FRACT_ERROR_DIST" 
( normInput IN NUMBER
) RETURN NUMBER AS

 -------------------------------------------------------------------------------
  -- implementation of fractional error distribution
  -- JWS@20090601 - First rev.
  -------------------------------------------------------------------------------
  -- temp var
  t1 NUMBER:= 1.0 / (1.0 + 0.5 * ABS(normInput));
  
  -- exponent input to next equation
  exponent_input NUMBER:= -normInput*normInput - 1.26551223 + 
                           t1*(1.00002368 + t1*(0.37409196 + t1*(0.09678418 + t1*(-0.18628806 + t1*(0.27886807 + t1*(-1.13520398 + t1*(1.48851587 + t1*(-0.82215223 + t1*(0.17087277)))))))));
  -- Horner's method
  ans NUMBER:= 1 - t1 * EXP(exponent_input);

  fractError NUMBER;

BEGIN
  -- handle sign
  IF normInput>0 THEN fractError:= ans; ELSE fractError:= -ans; END IF;
  return fractError;

END FRACT_ERROR_DIST;

 
 
 
 
 
 
 
/
 
