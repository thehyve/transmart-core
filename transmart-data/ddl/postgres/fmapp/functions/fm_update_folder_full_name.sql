--
-- Name: fm_update_folder_full_name(); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION fm_update_folder_full_name() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_folder_full_name character varying(1000);
  folder_ids CURSOR is
    select folder_id
    from fm_folder;
    
begin
  for folder_rec in folder_ids
  loop
    select fm_get_folder_full_name(folder_rec.folder_id) into v_folder_full_name
    from dual;
    
    update fm_folder set folder_full_name = v_folder_full_name
    where folder_id = folder_rec.folder_id;
  end loop;
end;
$$;

