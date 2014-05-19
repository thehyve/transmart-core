--
-- Name: is_number(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION is_number ( p_string in character varying)
  RETURNS numeric AS $body$
DECLARE

        l_number numeric;
    
BEGIN
        l_number := p_string;
        return 0;
   exception
       when others then
           return 1;
   end;

$body$
LANGUAGE PLPGSQL;

