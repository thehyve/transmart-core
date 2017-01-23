--
-- Name: is_date(character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION is_date(character varying, character varying) RETURNS numeric
    LANGUAGE plpgsql IMMUTABLE STRICT
    AS $_$
declare
        i numeric;
begin
        i := $1::date;
        return 0;
        EXCEPTION WHEN invalid_text_representation then
                return 1;
end;
$_$;

