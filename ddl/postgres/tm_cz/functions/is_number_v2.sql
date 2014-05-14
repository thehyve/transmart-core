--
-- Name: is_number_v2(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION is_number_v2 ( p_string in text)
  RETURNS bigint AS $body$
DECLARE

        l_number bigint;
    
BEGIN
        l_number := p_string;
        return l_number;
   exception
       when others then
           return 1;
   end;

$body$
LANGUAGE PLPGSQL;
