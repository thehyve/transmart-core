--
-- update permissions for schema searchapp
--

set search_path = searchapp, pg_catalog;

GRANT ALL ON ALL TABLES IN SCHEMA searchapp TO tm_cz;
GRANT ALL ON ALL TABLES IN SCHEMA searchapp TO biomart_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA searchapp TO tm_cz;
GRANT ALL ON ALL SEQUENCES IN SCHEMA searchapp TO biomart_user;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA searchapp TO tm_cz;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA searchapp TO biomart_user;

