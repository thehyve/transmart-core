--
-- search_required_upload_field moved from tm_cz to searchapp for cleaner permissions
--

set search_path = searchapp, pg_catalog;

\i ../../../ddl/postgres/searchapp/search_required_upload_field.sql

ALTER TABLE searchapp.search_required_upload_field OWNER TO searchapp, SET TABLESPACE transmart;
GRANT ALL ON searchapp.search_required_upload_field TO biomart_user;

ALTER INDEX search_reqd_up_field_pk SET TABLESPACE indx;

ALTER SEQUENCE searchapp.seq_req_upload_field_id OWNER TO searchapp;
GRANT ALL ON SEQUENCE searchapp.seq_req_upload_field_id TO biomart_user;
