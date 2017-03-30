--
-- Name: bio_patient_event; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_patient_event (
    bio_patient_event_id bigint NOT NULL,
    bio_patient_id bigint NOT NULL,
    event_code character varying(200),
    event_type_code character varying(200),
    event_date timestamp without time zone,
    site character varying(400),
    bio_clinic_trial_timepoint_id bigint NOT NULL
);

--
-- Name: bio_patient_event bio_patient_event_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event
    ADD CONSTRAINT bio_patient_event_pk PRIMARY KEY (bio_patient_event_id);

--
-- Name: tf_trg_bio_patient_event_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_patient_event_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_PATIENT_EVENT_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_PATIENT_EVENT_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_patient_event trg_bio_patient_event_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_patient_event_id BEFORE INSERT ON bio_patient_event FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_patient_event_id();

--
-- Name: bio_patient_event bio_pt_event_bio_pt_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event
    ADD CONSTRAINT bio_pt_event_bio_pt_fk FOREIGN KEY (bio_patient_id) REFERENCES bio_patient(bio_patient_id);

--
-- Name: bio_patient_event bio_pt_event_bio_trl_tp_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient_event
    ADD CONSTRAINT bio_pt_event_bio_trl_tp_fk FOREIGN KEY (bio_clinic_trial_timepoint_id) REFERENCES bio_clinc_trial_time_pt(bio_clinc_trial_tm_pt_id);

