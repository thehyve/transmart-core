--
-- Name: num_occurances(text, text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION num_occurances (
  input_str character varying,
  search_str character varying
)  RETURNS integer AS $body$
DECLARE

  num integer;

BEGIN
  num := 0;
  while instr(input_str, search_str, 1, num + 1) > 0 loop
    num := num + 1;
  end loop;
  return num;
end;

$body$
LANGUAGE PLPGSQL;
