CREATE OR REPLACE VIEW BIOMART.VW_FACETED_SEARCH_FILE AS
select f.folder_id as FOLDER_ID
, fa.object_uid as OBJECT_UID
, bio_data_id as STUDY_ID
, trim(leading ' ' from listagg(to_char(fi.display_name || ' ' || fi.description),' ') within group (order by fi.display_name)) as filenames
from fmapp.fm_folder f
inner join fmapp.fm_folder_association fa on f.folder_id = fa.folder_id
inner join biomart.bio_data_uid buid 
ON fa.object_uid = buid.unique_id
inner join biomart.bio_experiment be 
ON buid.bio_data_id = be.bio_experiment_id
left outer join fmapp.fm_folder_file_association ffa on f.folder_id = ffa.folder_id
left outer join fmapp.fm_file fi on fi.file_id = ffa.file_id
group by f.folder_id
, fa.object_uid 
, bio_data_id ;