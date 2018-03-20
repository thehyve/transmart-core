-- Add columns to the query table for storing additional information with user preferences
ALTER TABLE BIOMART_USER.QUERY
 ADD (QUERY_BLOB  CLOB);

COMMENT ON COLUMN biomart_user.query.query_blob IS 'Additional information with user preferences e.g. data table state';
