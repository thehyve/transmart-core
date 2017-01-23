--
-- Type: FUNCTION; Owner: TM_CZ; Name: IS_NUMBER_V2
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."IS_NUMBER_V2" 
 ( p_string in varchar2)
 return number
    as
        l_number number;
    begin
        l_number := p_string;
        return l_number;
   exception
       when others then
           return 1;
   end;

/
 
