-- Add columns to the query table for subscription feature
alter table biomart_user.query
  add column query_blob text;

COMMENT ON COLUMN biomart_user.query.query_blob IS 'Additional information with user preferences e.g. data table state';
