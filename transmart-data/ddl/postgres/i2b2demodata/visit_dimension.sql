--
-- Name: visit_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE visit_dimension (
    encounter_num numeric(38,0) NOT NULL,
    patient_num numeric(38,0) NOT NULL,
    active_status_cd character varying(50),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    inout_cd character varying(50),
    location_cd character varying(50),
    location_path character varying(900),
    length_of_stay numeric(38,0),
    visit_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0)
);

--
-- Name: visit_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY visit_dimension
    ADD CONSTRAINT visit_dimension_pk PRIMARY KEY (encounter_num, patient_num);

--
-- Name: vd_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX vd_uploadid_idx ON visit_dimension USING btree (upload_id);

--
-- Name: visitdim_en_pn_lp_io_sd_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX visitdim_en_pn_lp_io_sd_idx ON visit_dimension USING btree (encounter_num, patient_num, location_path, inout_cd, start_date, end_date, length_of_stay);

--
-- Name: visitdim_std_edd_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX visitdim_std_edd_idx ON visit_dimension USING btree (start_date, end_date);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.visit_dimension IS 'Table holds descriptions of actual visits in real time.';

COMMENT ON COLUMN visit_dimension.encounter_num IS 'Primary key. Id of the visit. Referred to by the encounter_num column of observation_fact.';
COMMENT ON COLUMN visit_dimension.patient_num IS 'Primary key. Id linking to patient_num in the patient_dimension.';
COMMENT ON COLUMN visit_dimension.start_date IS 'Start date and time of the visit.';
COMMENT ON COLUMN visit_dimension.end_date IS 'End date and time of the visit.';
