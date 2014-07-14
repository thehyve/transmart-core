--
-- Name: _final_median(anyarray); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION _final_median(anyarray) RETURNS numeric
    LANGUAGE sql IMMUTABLE
    AS $_$
   SELECT AVG(val)
   FROM (
     SELECT val
     FROM unnest($1) val
     ORDER BY 1
     LIMIT  2 - MOD(array_upper($1, 1), 2)
     OFFSET CEIL(array_upper($1, 1) / 2.0) - 1
   ) sub;
$_$;

--
-- Name: _final_median(double precision[]); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION _final_median(double precision[]) RETURNS double precision
    LANGUAGE sql IMMUTABLE
    AS $_$
   SELECT AVG(val)
   FROM (
     SELECT val
     FROM unnest($1) val
     ORDER BY 1
     LIMIT  2 - MOD(array_upper($1, 1), 2)
     OFFSET CEIL(array_upper($1, 1) / 2.0) - 1
   ) sub;
$_$;

