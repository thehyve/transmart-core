--
-- Name: encounter_mapping; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE encounter_mapping (
    encounter_ide character varying(200) NOT NULL,
    encounter_ide_source character varying(50) NOT NULL,
    encounter_num numeric(38,0) NOT NULL,
    patient_ide character varying(200),
    patient_ide_source character varying(50),
    encounter_ide_status character varying(50),
    upload_date timestamp without time zone,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0)
);

--
-- Name: encounter_mapping_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY encounter_mapping
    ADD CONSTRAINT encounter_mapping_pk PRIMARY KEY (encounter_ide, encounter_ide_source);

--
-- Name: em_encnum_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX em_encnum_idx ON encounter_mapping USING btree (encounter_num);

--
-- Name: em_idx_encpath; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX em_idx_encpath ON encounter_mapping USING btree (encounter_ide, encounter_ide_source, patient_ide, patient_ide_source, encounter_num);

--
-- Name: em_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX em_uploadid_idx ON encounter_mapping USING btree (upload_id);

