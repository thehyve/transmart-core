ALTER TABLE i2b2demodata.async_job ADD user_id VARCHAR2(50 BYTE);
COMMENT ON COLUMN i2b2demodata.async_job.user_id IS 'Name of the user.';
