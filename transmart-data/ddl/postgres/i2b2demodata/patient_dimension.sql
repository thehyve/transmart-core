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
    zip_cd character varying(50),
    statecityzip_path character varying(700),
    income_cd character varying(50),
    patient_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(107),
    upload_id numeric(38,0)
);

--
-- Name: patient_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY patient_dimension
    ADD CONSTRAINT patient_dimension_pk PRIMARY KEY (patient_num);

--
-- Name: idx_pd_sourcesystemcd_pnum; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_pd_sourcesystemcd_pnum ON patient_dimension USING btree (sourcesystem_cd, patient_num);

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

--
-- Name: tf_trg_patient_dimension(); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION tf_trg_patient_dimension() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.PATIENT_NUM is null then
 select nextval('i2b2demodata.SEQ_PATIENT_NUM') into NEW.PATIENT_NUM ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_patient_dimension; Type: TRIGGER; Schema: i2b2demodata; Owner: -
--
CREATE TRIGGER trg_patient_dimension BEFORE INSERT ON patient_dimension FOR EACH ROW EXECUTE PROCEDURE tf_trg_patient_dimension();

--
-- Name: seq_patient_num; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE seq_patient_num
    START WITH 1000384597
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.patient_dimension IS 'Table holds patients.';

COMMENT ON COLUMN patient_dimension.patient_num IS 'Primary key. Id of the patient.';
COMMENT ON COLUMN patient_dimension.sex_cd IS 'One of [male, female, unknown].';
