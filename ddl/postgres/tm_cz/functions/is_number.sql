--
-- Name: is_number(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION is_number ( p_string in text)
  RETURNS bigint AS $body$
DECLARE

        l_number bigint;
    
BEGIN
        l_number := p_string;
        return 0;
   exception
       when others then
           return 1;
   end;

$body$
LANGUAGE PLPGSQL;

