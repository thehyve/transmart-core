--
-- Type: PROCEDURE; Owner: FMAPP; Name: FM_UPDATE_FOLDER_FULL_NAME
--
  CREATE OR REPLACE PROCEDURE "FMAPP"."FM_UPDATE_FOLDER_FULL_NAME" 
as
  v_folder_full_name nvarchar2(1000);
  cursor folder_ids is
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
/
 
