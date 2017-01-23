--
-- add table deapp.de_gene_info
--

set search_path = deapp, pg_catalog;

\i ../../../ddl/postgres/deapp/de_gene_info.sql

ALTER TABLE deapp.de_gene_info OWNER TO deapp, SET TABLESPACE transmart;

ALTER SEQUENCE deapp.de_gene_info_gene_info_id_seq OWNER TO deapp;
ALTER SEQUENCE de_gene_info_gene_info_id_seq OWNED BY de_gene_info.gene_info_id;

GRANT ALL ON SEQUENCE deapp.de_gene_info_gene_info_id_seq TO deapp;
GRANT ALL ON SEQUENCE deapp.de_gene_info_gene_info_id_seq TO biomart_user;
