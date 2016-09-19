--
-- Name: dropsyn(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION dropsyn() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

 s_cur CURSOR FOR
 SELECT synonym_name
 FROM user_synonyms;

 RetVal  bigint;
 sqlstr  varchar(200);

BEGIN
  FOR s_rec IN s_cur LOOP
    sqlstr := 'DROP SYNONYM ' || s_rec.synonym_name;

    EXECUTE sqlstr;
    COMMIT;
  END LOOP;
END dropsyn;

$$;

