--
-- Type: FUNCTION; Owner: TM_CZ; Name: IS_DATE
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."IS_DATE" 
 ( p_string in varchar2
 , d_fmt varchar2 := 'YYYYMMDD')
 return number
    as
        x_date date;
    begin
        x_date := to_date(p_string,d_fmt);
        return 0;
   exception
       when others then
           return 1;
   end;


/
 
