--
-- Type: FUNCTION; Owner: TM_CZ; Name: NUM_OCCURANCES
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."NUM_OCCURANCES" (
  input_str nvarchar2,
  search_str nvarchar2
) return number
as
  num number;
begin
  num := 0;
  while instr(input_str, search_str, 1, num + 1) > 0 loop
    num := num + 1;
  end loop;
  return num;
end;




/
