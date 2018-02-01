--
-- Name: patient_mapping; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE patient_mapping (
    patient_ide character varying(200) NOT NULL,
    patient_ide_source character varying(50) NOT NULL,
    patient_num numeric(38,0) NOT NULL,
    patient_ide_status character varying(50),
    upload_date timestamp without time zone,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0)
);

--
-- Name: patient_mapping_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY patient_mapping
    ADD CONSTRAINT patient_mapping_pk PRIMARY KEY (patient_ide, patient_ide_source);

--
-- Name: pm_encpnum_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pm_encpnum_idx ON patient_mapping USING btree (patient_ide, patient_ide_source, patient_num);

--
-- Name: pm_patnum_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pm_patnum_idx ON patient_mapping USING btree (patient_num);

--
-- Name: pm_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pm_uploadid_idx ON patient_mapping USING btree (upload_id);

--
-- Table documentation
--
COMMENT ON TABLE i2b2demodata.patient_mapping IS 'Table with subject identifiers from different sources.';

COMMENT ON COLUMN patient_mapping.patient_ide IS 'Primary key. Subject identifier associated with a patient.';
COMMENT ON COLUMN patient_mapping.patient_ide_source IS 'Primary key. Source of the subject identifier.';
COMMENT ON COLUMN patient_mapping.patient_num IS 'The id of the patient in TranSMART. Refers to patient_num in patient_dimension.';
