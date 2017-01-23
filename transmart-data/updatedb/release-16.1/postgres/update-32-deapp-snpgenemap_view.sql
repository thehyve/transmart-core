--
-- replace table deapp.de_snp_gene_map with a view
--

set search_path = deapp, pg_catalog;

DROP TABLE deapp.de_snp_gene_map;

\i ../../../ddl/postgres/deapp/views/de_snp_gene_map.sql

ALTER VIEW deapp.de_snp_gene_map OWNER TO deapp;
