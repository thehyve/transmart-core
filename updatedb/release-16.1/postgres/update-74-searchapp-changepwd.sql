--
-- column for new Utilities "change password" option
--

set search_path = searchapp, pg_catalog;

ALTER TABLE searchapp.search_auth_user ADD COLUMN change_passwd boolean;
