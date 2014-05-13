--
-- Type: FUNCTION; Owner: AMAPP; Name: AM_TAG_VALUE_UID
--
CREATE FUNCTION am_tag_value_uid (tag_value_id bigint) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
begin
  return 'TAG:' || TO_CHAR(tag_value_id,FM99999999999999999);
end;
$$;
 
