--
-- Type: FUNCTION; Owner: AMAPP; Name: AM_TAG_VALUE_UID
--
  CREATE OR REPLACE FUNCTION "AMAPP"."AM_TAG_VALUE_UID" (
  tag_value_id number
) return varchar2
as
begin
  return 'TAG:' || to_char(tag_value_id);
end;
/
 
