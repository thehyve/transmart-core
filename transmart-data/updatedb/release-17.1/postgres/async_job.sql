ALTER TABLE ONLY i2b2demodata.async_job ADD COLUMN user_id character varying(50);
COMMENT ON COLUMN i2b2demodata.async_job.user_id IS 'Name of the user.';
