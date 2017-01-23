--
-- Type: PROCEDURE; Owner: TM_CZ; Name: LOAD_KEGG_CONTENT_DATA
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."LOAD_KEGG_CONTENT_DATA" 
as
begin

begin

  delete from bio_content_reference
    where bio_content_id in
      (select bio_file_content_id
      from bio_content
      where repository_id in
        (select bio_content_repo_id
        from bio_content_repository
        where upper(repository_type)='KEGG')
      );
  --806
  delete from bio_content
    where repository_id =
      (select bio_content_repo_id
      from bio_content_repository
      where upper(repository_type)='KEGG');
  --806
  delete from bio_content_repository
    where upper(repository_type)='KEGG';
  --1
commit;
end;

begin
-- populate bio_content_repository
  insert into bio_content_repository(
    location
  , active_y_n
  , repository_type
  , location_type
  )
  values (
    'http://www.genome.jp/'
  , 'Y'
  , 'Kegg'
  , 'URL'
  );
commit;
end;

begin

  insert into bio_content(
  --  file_name
    repository_id
  , location
  --, title  , abstract
  , file_type
  --, etl_id
  )
  select distinct
    bcr.bio_content_repo_id
  , bcr.location||'dbget-bin/show_pathway?'|| bm.primary_external_id
  , 'Data'
  from
    bio_content_repository bcr
  , bio_marker bm
  where upper(bcr.repository_type)='KEGG'
  and upper(bm.primary_source_code)='KEGG';
  --806 rows inserted
commit;
end;

begin

  insert into bio_content_reference(
    bio_content_id
  , bio_data_id
  , content_reference_type
  )
  select distinct
    bc.bio_file_content_id
  , path.bio_marker_id
  , bcr.location_type
  from
    bio_content bc
  , bio_marker path
  , bio_content_repository bcr
  where bc.repository_id = bcr.bio_content_repo_id
  and path.primary_external_id=substr(bc.location, length(bc.location)-7)
  and path.primary_source_code='KEGG';
  --806
commit;
end;


end;



/
 
