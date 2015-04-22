--
-- Name: observation_fact; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE observation_fact (
    encounter_num numeric(38,0),
    patient_num numeric(38,0) NOT NULL,
    concept_cd character varying(50) NOT NULL,
    provider_id character varying(50) NOT NULL,
    start_date timestamp without time zone,
    modifier_cd character varying(100) NOT NULL,
    instance_num numeric(18,0),
    valtype_cd character varying(50),
    tval_char character varying(255),
    nval_num numeric(18,5),
    valueflag_cd character varying(50),
    quantity_num numeric(18,5),
    units_cd character varying(50),
    end_date timestamp without time zone,
    location_cd character varying(50),
    observation_blob text,
    confidence_num numeric(18,5),
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0),
    sample_cd character varying(200)
);

--
-- Name: observation_fact_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY observation_fact
    ADD CONSTRAINT observation_fact_pkey PRIMARY KEY (patient_num, concept_cd, provider_id, modifier_cd);

--
-- Name: fact_modifier_patient; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX fact_modifier_patient ON observation_fact USING btree (modifier_cd, patient_num);

--
-- Name: idx_fact_concept; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_fact_concept ON observation_fact USING btree (concept_cd);

--
-- Name: idx_fact_cpe; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_fact_cpe ON observation_fact USING btree (concept_cd, patient_num, encounter_num);

--
-- Name: idx_fact_patient_num; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX idx_fact_patient_num ON observation_fact USING btree (patient_num);

--
-- Name: tf_trg_encounter_num(); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE FUNCTION tf_trg_encounter_num() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ENCOUNTER_NUM is null then
 select nextval('i2b2demodata.SEQ_ENCOUNTER_NUM') into NEW.ENCOUNTER_NUM ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_encounter_num; Type: TRIGGER; Schema: i2b2demodata; Owner: -
--
CREATE TRIGGER trg_encounter_num BEFORE INSERT ON observation_fact FOR EACH ROW EXECUTE PROCEDURE tf_trg_encounter_num();

--
-- Name: observation_fact_concept_cd_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY observation_fact
    ADD CONSTRAINT observation_fact_concept_cd_fk FOREIGN KEY (concept_cd) REFERENCES concept_dimension(concept_cd) ON DELETE CASCADE;

--
-- Name: observation_fact_modifier_cd_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY observation_fact
    ADD CONSTRAINT observation_fact_modifier_cd_fk FOREIGN KEY (modifier_cd) REFERENCES modifier_dimension(modifier_cd) ON DELETE CASCADE;

--
-- Name: observation_fact_patient_num_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY observation_fact
    ADD CONSTRAINT observation_fact_patient_num_fk FOREIGN KEY (patient_num) REFERENCES patient_dimension(patient_num) ON DELETE CASCADE;

--
-- Name: seq_encounter_num; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE seq_encounter_num
    START WITH 49814595
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

