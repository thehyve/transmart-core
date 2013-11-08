--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_SHOW_NODE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_SHOW_NODE" 
(
  path VARCHAR2
)
AS
BEGIN

  -------------------------------------------------------------
  -- Shows a tree node in I2b2
  -- KCR@20090519 - First Rev
  -------------------------------------------------------------
  if path != ''  or path != '%'
  then

      --I2B2
    UPDATE i2b2
      SET c_visualattributes = 'FA'
    WHERE c_visualattributes LIKE 'F%'
      AND C_FULLNAME LIKE PATH || '%';

     UPDATE i2b2
      SET c_visualattributes = 'LA'
    WHERE c_visualattributes LIKE 'L%'
      AND C_FULLNAME LIKE PATH || '%';
    COMMIT;
  END IF;

END;
/
 
