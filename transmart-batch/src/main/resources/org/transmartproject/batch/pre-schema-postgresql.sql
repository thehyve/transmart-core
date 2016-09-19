CREATE SCHEMA ts_batch;
ALTER DEFAULT PRIVILEGES IN SCHEMA ts_batch GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tm_cz;
ALTER DEFAULT PRIVILEGES IN SCHEMA ts_batch GRANT USAGE ON SEQUENCES TO tm_cz;
ALTER DEFAULT PRIVILEGES IN SCHEMA ts_batch GRANT EXECUTE ON FUNCTIONS TO tm_cz;
ALTER ROLE tm_cz SET search_path = "$user",tm_wz,tm_lz,deapp,i2b2metadata,i2b2demodata,deapp,biomart,ts_batch;

SET search_path = ts_batch;
