--
-- Type: FUNCTION; Owner: TM_CZ; Name: IS_NUMBER
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."IS_NUMBER" 
 ( p_string in varchar2)
 return number
    as
        l_number number;
    begin
        l_number := p_string;
        return 0;
   exception
       when others then
           return 1;
   end;




/
 
