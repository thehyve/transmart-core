--
-- drop form_layout from tm_cz, move to searchapp for cleaner permissions
--

set search_path = tm_cz, pg_catalog;

DROP TRIGGER IF EXISTS trg_cz_form_layout_id ON tm_cz.cz_form_layout;

DROP TABLE IF EXISTS tm_cz.cz_form_layout;

DROP FUNCTION IF EXISTS tm_cz.tf_trg_cz_form_layout_id();

DROP SEQUENCE IF EXISTS tm_cz.cz_form_layout_seq;

DROP SEQUENCE IF EXISTS tm_cz.seq_form_layout_id;
