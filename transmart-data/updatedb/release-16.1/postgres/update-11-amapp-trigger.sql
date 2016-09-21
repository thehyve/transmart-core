--
-- Explicit schema name in trigger function code so it can be run by biomart_user
-- through transmartApp to add and update the browse tab metadata
--

set search_path = amapp, pg_catalog;

--
-- Name: tf_trg_am_tag_value_uid(); Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE OR REPLACE FUNCTION tf_trg_am_tag_value_uid() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM amapp.am_data_uid 
  WHERE am_data_id = new.tag_value_id;
  
  if rec_count = 0 then
    insert into amapp.am_data_uid (am_data_id, unique_id, am_data_type)
    values (NEW.tag_value_id, amapp.am_tag_value_uid(NEW.tag_value_id), 'AM_TAG_VALUE');
  end if;
RETURN NEW;
end;
$$;
