--
-- sequence cache 1 to increment by 1 when used
--

set search_path = searchapp, pg_catalog;

ALTER SEQUENCE searchapp.hibernate_sequence cache 1;
