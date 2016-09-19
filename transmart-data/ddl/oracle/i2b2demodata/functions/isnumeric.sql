--
-- Type: FUNCTION; Owner: I2B2DEMODATA; Name: ISNUMERIC
--
  CREATE OR REPLACE FUNCTION "I2B2DEMODATA"."ISNUMERIC" 
  ( p_string in varchar2)
  return number
  as
      l_number number;
  begin
      l_number := p_string;
      return 1;
  exception
      when others then
          return 0;
  end;
 
 
 
 
 
 
 
 
 
 
/
 
