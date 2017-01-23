--
-- Name: czf_num_occurances(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czf_num_occurances(input_str character varying, search_str character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE

  num integer;

BEGIN
  num := 0;
  while instr(input_str, search_str, 1, num + 1) > 0 loop
    num := num + 1;
  end loop;
  return num;
end;
 
$$;

