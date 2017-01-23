--
-- Name: i2b2_show_node(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_show_node(path character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
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
 
$$;

