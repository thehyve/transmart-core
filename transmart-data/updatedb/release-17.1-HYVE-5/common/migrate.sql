-- Merge EXPORT and OWN access levels
update searchapp.search_auth_sec_object_access
set secure_access_level_id = (select search_sec_access_level_id from searchapp.search_sec_access_level where access_level_name='EXPORT')
where secure_access_level_id = (select search_sec_access_level_id from searchapp.search_sec_access_level where access_level_name='OWN');

update searchapp.search_auth_user_sec_access
set search_sec_access_level_id = (select search_sec_access_level_id from searchapp.search_sec_access_level where access_level_name='EXPORT')
where search_sec_access_level_id = (select search_sec_access_level_id from searchapp.search_sec_access_level where access_level_name='OWN');

-- Delete OWN, rename EXPORT to MEASUREMENTS, rename VIEW to SUMMARY
delete from searchapp.search_sec_access_level where access_level_name='OWN';
update searchapp.search_sec_access_level set access_level_name='MEASUREMENTS', access_level_value=10 where access_level_name='EXPORT';
update searchapp.search_sec_access_level set access_level_name='SUMMARY', access_level_value=5 where access_level_name='VIEW';

-- Create COUNTS_WITH_THRESHOLD access level
insert into searchapp.search_sec_access_level(access_level_name, access_level_value) values ('COUNTS_WITH_THRESHOLD', 1);
