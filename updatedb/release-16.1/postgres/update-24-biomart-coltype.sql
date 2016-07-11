--
-- replace view with code translated from oracle
--

set search_path = biomart, pg_catalog;

-- drop view to change type

DROP VIEW biomart.vw_faceted_search_disease;

\i ../../../ddl/postgres/biomart/views/vw_faceted_search_disease.sql

ALTER VIEW vw_faceted_search_disease OWNER TO biomart;
