--
-- Name: is_number_v2(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION is_number_v2(p_string character varying) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE

        l_number numeric;
    
BEGIN
        l_number := p_string;
        return l_number;
   exception
       when others then
           return 1;
   end;

$$;

