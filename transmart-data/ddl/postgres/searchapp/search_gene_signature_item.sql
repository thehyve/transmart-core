--
-- Name: search_gene_signature_item; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_gene_signature_item (
    search_gene_signature_id bigint NOT NULL,
    bio_marker_id bigint,
    fold_chg_metric bigint,
    bio_data_unique_id character varying(200),
    id bigint NOT NULL,
    bio_assay_feature_group_id bigint,
    probeset_id bigint
);

--
-- Name: search_gene_signature_ite_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature_item
    ADD CONSTRAINT search_gene_signature_ite_pk PRIMARY KEY (id);

