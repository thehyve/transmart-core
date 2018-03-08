-- Add columns to the query table for subscription feature
alter table biomart_user.query
  add column subscribed boolean,
  add column subscription_freq character varying(25);

COMMENT ON COLUMN biomart_user.query.subscribed IS 'Flag to indicate if the user has subscribed to the query.';
COMMENT ON COLUMN biomart_user.query.subscription_freq IS 'Frequency of query notifications: "DAILY" or "WEEKLY".';


set search_path = biomart_user, pg_catalog;
\i ../../../ddl/postgres/biomart_user/query_set.sql
\i ../../../ddl/postgres/biomart_user/query_set_diff.sql
\i ../../../ddl/postgres/biomart_user/query_set_instance.sql

