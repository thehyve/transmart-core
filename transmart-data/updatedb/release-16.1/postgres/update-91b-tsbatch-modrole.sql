--
-- 
--

ALTER ROLE ts_batch WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION;
ALTER ROLE tm_cz SET search_path TO "$user", tm_wz, tm_lz, deapp, i2b2metadata, i2b2demodata, deapp, biomart, ts_batch;


