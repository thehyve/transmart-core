--
-- add table deapp.de_gene_source
--

set search_path = deapp, pg_catalog;

\i ../../../ddl/postgres/deapp/de_gene_source.sql

ALTER TABLE deapp.de_gene_source OWNER TO deapp, SET TABLESPACE transmart;

ALTER INDEX deapp.de_gene_info_entrez_id_gene_source_id_idx SET TABLESPACE indx;
ALTER INDEX deapp.de_gene_info_gene_symbol_idx SET TABLESPACE indx;

ALTER INDEX deapp.de_gene_source_pkey SET TABLESPACE indx;
ALTER INDEX deapp.u_gene_source_name SET TABLESPACE indx;

