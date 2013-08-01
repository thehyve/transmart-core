--
-- Name: patient_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE patient_dimension (
    patient_num numeric(38,0) NOT NULL,
    vital_status_cd character varying(50),
    birth_date timestamp without time zone,
    death_date timestamp without time zone,
    sex_cd character varying(50),
    age_in_years_num numeric(38,0),
    language_cd character varying(50),
    race_cd character varying(50),
    marital_status_cd character varying(50),
    religion_cd character varying(50),
    zip_cd character varying(10),
    statecityzip_path character varying(700),
    income_cd character varying(50),
    patient_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0)
);

--
-- Name: patient_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY patient_dimension
    ADD CONSTRAINT patient_dimension_pk PRIMARY KEY (patient_num);

--
-- Name: patd_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX patd_uploadid_idx ON patient_dimension USING btree (upload_id);

--
-- Name: pd_idx_allpatientdim; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pd_idx_allpatientdim ON patient_dimension USING btree (patient_num, vital_status_cd, birth_date, death_date, sex_cd, age_in_years_num, language_cd, race_cd, marital_status_cd, religion_cd, zip_cd, income_cd);

--
-- Name: pd_idx_dates; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pd_idx_dates ON patient_dimension USING btree (patient_num, vital_status_cd, birth_date, death_date);

--
-- Name: pd_idx_statecityzip; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pd_idx_statecityzip ON patient_dimension USING btree (statecityzip_path, patient_num);

