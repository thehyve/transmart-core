--
-- wider column for URL
-- old size 600
-- new size 4000
--

set search_path = i2b2demodata, pg_catalog;

ALTER TABLE i2b2demodata.async_job ALTER COLUMN alt_viewer_url TYPE character varying(4000);
