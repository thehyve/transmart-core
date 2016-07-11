--
-- form_layout moved from tm_cz to searchapp for clean permissions
--

set search_path = searchapp, pg_catalog;

\i ../../../ddl/postgres/searchapp/search_form_layout.sql

ALTER TABLE searchapp.search_form_layout OWNER TO searchapp, SET TABLESPACE transmart;
GRANT ALL ON searchapp.search_form_layout TO biomart_user;

ALTER INDEX searchapp.search_form_layout_pk SET TABLESPACE indx;

ALTER SEQUENCE searchapp.seq_search_form_layout_id OWNER TO searchapp;
GRANT ALL ON SEQUENCE searchapp.seq_search_form_layout_id TO biomart_user;
