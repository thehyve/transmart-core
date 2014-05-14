--
-- Name: isnumeric(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION isnumeric ( p_string in text)
   RETURNS bigint AS $body$
DECLARE

      l_number bigint;
  
BEGIN
      l_number := p_string;
      return 1;
  exception
      when others then
          return 0;
  end;

$body$
LANGUAGE PLPGSQL;
