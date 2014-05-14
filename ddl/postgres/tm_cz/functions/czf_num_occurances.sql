--
-- Name: czf_num_occurances(text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION czf_num_occurances (
  input_str text,
  search_str text
)  RETURNS bigint AS $body$
DECLARE

  num bigint;

BEGIN
  num := 0;
  while instr(input_str, search_str, 1, num + 1) > 0 loop
    num := num + 1;
  end loop;
  return num;
end;
 
$body$
LANGUAGE PLPGSQL;
