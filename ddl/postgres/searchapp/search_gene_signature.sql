--
-- Name: search_gene_signature; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_gene_signature (
    search_gene_signature_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(1000),
    unique_id character varying(50),
    create_date timestamp without time zone NOT NULL,
    created_by_auth_user_id bigint NOT NULL,
    last_modified_date timestamp without time zone,
    modified_by_auth_user_id bigint,
    version_number character varying(50),
    public_flag boolean DEFAULT false,
    deleted_flag boolean DEFAULT false,
    parent_gene_signature_id bigint,
    source_concept_id bigint,
    source_other character varying(255),
    owner_concept_id bigint,
    stimulus_description character varying(1000),
    stimulus_dosing character varying(255),
    treatment_description character varying(1000),
    treatment_dosing character varying(255),
    treatment_bio_compound_id bigint,
    treatment_protocol_number character varying(50),
    pmid_list character varying(255),
    species_concept_id bigint NOT NULL,
    species_mouse_src_concept_id bigint,
    species_mouse_detail character varying(255),
    tissue_type_concept_id bigint,
    experiment_type_concept_id bigint,
    experiment_type_in_vivo_descr character varying(255),
    experiment_type_atcc_ref character varying(255),
    analytic_cat_concept_id bigint,
    analytic_cat_other character varying(255),
    bio_assay_platform_id bigint NOT NULL,
    analyst_name character varying(100),
    norm_method_concept_id bigint,
    norm_method_other character varying(255),
    analysis_method_concept_id bigint,
    analysis_method_other character varying(255),
    multiple_testing_correction boolean,
    p_value_cutoff_concept_id bigint NOT NULL,
    upload_file character varying(255) NOT NULL,
    search_gene_sig_file_schema_id bigint DEFAULT 1 NOT NULL,
    fold_chg_metric_concept_id bigint NOT NULL, --was DEFAULT NULL NOT NULL changed on test server
    experiment_type_cell_line_id bigint,
    qc_performed numeric(1,0),
    qc_date date,
    qc_info character varying(255),
    data_source character varying(255),
    custom_value1 character varying(255),
    custom_name1 character varying(255),
    custom_value2 character varying(255),
    custom_name2 character varying(255),
    custom_value3 character varying(255),
    custom_name3 character varying(255),
    custom_value4 character varying(255),
    custom_name4 character varying(255),
    custom_value5 character varying(255),
    custom_name5 character varying(255),
    version character varying(255)
);

--
-- Name: search_gene_sig_descr_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT search_gene_sig_descr_pk PRIMARY KEY (search_gene_signature_id);

--
-- Name: gene_sig_create_auth_user_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT gene_sig_create_auth_user_fk1 FOREIGN KEY (created_by_auth_user_id) REFERENCES search_auth_user(id);

--
-- Name: gene_sig_file_schema_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT gene_sig_file_schema_fk1 FOREIGN KEY (search_gene_sig_file_schema_id) REFERENCES search_gene_sig_file_schema(search_gene_sig_file_schema_id);

--
-- Name: gene_sig_mod_auth_user_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT gene_sig_mod_auth_user_fk1 FOREIGN KEY (modified_by_auth_user_id) REFERENCES search_auth_user(id);

--
-- Name: gene_sig_parent_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT gene_sig_parent_fk1 FOREIGN KEY (parent_gene_signature_id) REFERENCES search_gene_signature(search_gene_signature_id);

