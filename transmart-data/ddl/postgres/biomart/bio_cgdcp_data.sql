--
-- Name: bio_cgdcp_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_cgdcp_data (
    evidence_code character varying(200),
    negation_indicator character(1),
    cell_line_id bigint,
    nci_disease_concept_code character varying(200),
    nci_role_code character varying(200),
    nci_drug_concept_code character varying(200),
    bio_data_id bigint NOT NULL
);

--
-- Name: bio_cancer_gene_curation_fact_; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_cgdcp_data
    ADD CONSTRAINT bio_cancer_gene_curation_fact_ PRIMARY KEY (bio_data_id);

--
-- Name: bio_cgdcp_data_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_cgdcp_data_pk ON bio_cgdcp_data USING btree (bio_data_id);

