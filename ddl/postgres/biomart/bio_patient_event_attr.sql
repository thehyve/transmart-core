--
-- Name: bio_patient_event_attr; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_patient_event_attr (
    bio_patient_attr_code character varying(200) NOT NULL,
    attribute_text_value character varying(200),
    attribute_numeric_value character varying(200),
    bio_clinic_trial_attr_id bigint NOT NULL,
    bio_patient_attribute_id bigint NOT NULL,
    bio_patient_event_id bigint NOT NULL
);

--
-- Name: bio_patient_attribute_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event_attr
    ADD CONSTRAINT bio_patient_attribute_pk PRIMARY KEY (bio_patient_attribute_id);

--
-- Name: bio_patient_event_attr_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_patient_event_attr_pk ON bio_patient_event_attr USING btree (bio_patient_attribute_id);

--
-- Name: tf_trg_bio_pt_evt_attr_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_pt_evt_attr_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CLINIC_TRIAL_ATTRIBUTE_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CLINIC_TRIAL_ATTRIBUTE_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_pt_evt_attr_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_pt_evt_attr_id BEFORE INSERT ON bio_patient_event_attr FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_pt_evt_attr_id();

--
-- Name: bio_pt_attr_trl_attr_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event_attr
    ADD CONSTRAINT bio_pt_attr_trl_attr_fk FOREIGN KEY (bio_clinic_trial_attr_id) REFERENCES bio_clinc_trial_attr(bio_clinc_trial_attr_id);

--
-- Name: bio_pt_event_attr_evt_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event_attr
    ADD CONSTRAINT bio_pt_event_attr_evt_fk FOREIGN KEY (bio_patient_event_id) REFERENCES bio_patient_event(bio_patient_event_id);

