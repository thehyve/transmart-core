--
-- Name: de_subject_protein_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_protein_data (
    trial_name character varying(50),
    protein_annotation_id bigint,
    component character varying(100),
    patient_id numeric(38,0),
    gene_symbol character varying(100),
    gene_id character varying(200),
    assay_id numeric,
    subject_id character varying(100),
    intensity numeric,
    zscore numeric,
    log_intensity numeric
);

--
-- Name: fk_protein_annotation_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_protein_data
    ADD CONSTRAINT fk_protein_annotation_id FOREIGN KEY (protein_annotation_id) REFERENCES de_protein_annotation(id);

