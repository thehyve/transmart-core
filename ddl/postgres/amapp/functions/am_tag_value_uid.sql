--
-- Name: am_tag_value_uid(bigint); Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE FUNCTION am_tag_value_uid(tag_value_id bigint) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
begin
  return 'TAG:' || tag_value_id::text;
end;
$$;

