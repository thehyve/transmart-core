--
-- Name: czf_parse_nth_value(character varying, numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czf_parse_nth_value(pvalue character varying, location numeric, delimiter character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE

   v_posA integer;
   v_posB integer;


BEGIN

   if location = 1 then
      v_posA := 1; -- Start at the beginning
   else
      v_posA := instr (pValue, delimiter, 1, location - 1);
      if v_posA = 0 then
         return null; --No values left.
      end if;
      v_posA := v_posA + length(delimiter);
   end if;

   v_posB := instr (pValue, delimiter, 1, location);
   if v_posB = 0 then -- Use the end of the file
      return substring(pValue from v_posA);
   end if;

   return substr (pValue, v_posA, v_posB - v_posA);

end;
 
$$;

