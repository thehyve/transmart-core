-- Add columns to the query table for subscription feature
ALTER TABLE BIOMART_USER.QUERY
 ADD (SUBSCRIBED  CHAR(1 BYTE))
 ADD (SUBSCRIPTION_FREQ  VARCHAR2(25 BYTE));

COMMENT ON COLUMN biomart_user.query.subscribed IS 'Flag to indicate if the user has subscribed to the query.';
COMMENT ON COLUMN biomart_user.query.subscription_freq IS 'Frequency of query notifications: "DAILY" or "WEEKLY".';
