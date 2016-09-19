--
-- Name: fm_get_folder_full_name(bigint); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION fm_get_folder_full_name(p_folder_id bigint) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_parent_id bigint;
  v_folder_full_name character varying(1000);
begin

  select parent_id into v_parent_id
  from fm_folder
  where folder_id = p_folder_id;
  
  v_folder_full_name := fm_folder_uid(p_folder_id) || '\';
  
  while v_parent_id is not null
  loop
    v_folder_full_name := fm_folder_uid(v_parent_id) || '\' || v_folder_full_name;

    select parent_id into v_parent_id
    from fm_folder
    where folder_id = v_parent_id;
  end loop;

  v_folder_full_name := '\' || v_folder_full_name;
  
  return v_folder_full_name;  
end;
$$;

