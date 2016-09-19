--
-- Name: search_gene_sig_file_schema; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_gene_sig_file_schema (
    search_gene_sig_file_schema_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(255),
    number_columns bigint DEFAULT 2 NOT NULL,
    supported boolean DEFAULT false
);

--
-- Name: search_gene_sig_file_sche_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_sig_file_schema
    ADD CONSTRAINT search_gene_sig_file_sche_pk PRIMARY KEY (search_gene_sig_file_schema_id);

