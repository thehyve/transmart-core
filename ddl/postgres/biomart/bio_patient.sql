--
-- Name: bio_patient; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_patient (
    bio_patient_id bigint NOT NULL,
    first_name character varying(200),
    last_name character varying(200),
    middle_name character varying(200),
    birth_date timestamp without time zone,
    birth_date_orig character varying(200),
    gender_code character varying(200),
    race_code character varying(200),
    ethnic_group_code character varying(200),
    address_zip_code character varying(200),
    country_code character varying(200),
    informed_consent_code character varying(200),
    bio_experiment_id bigint,
    bio_clinical_trial_p_group_id bigint
);

--
-- Name: bio_patient_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient
    ADD CONSTRAINT bio_patient_pk PRIMARY KEY (bio_patient_id);

--
-- Name: tf_trg_bio_patient_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_patient_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_PATIENT_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_PATIENT_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_patient_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_patient_id BEFORE INSERT ON bio_patient FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_patient_id();

--
-- Name: bio_patient_bio_clinic_tri_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient
    ADD CONSTRAINT bio_patient_bio_clinic_tri_fk FOREIGN KEY (bio_clinical_trial_p_group_id) REFERENCES bio_clinc_trial_pt_group(bio_clinical_trial_p_group_id);

--
-- Name: bio_patient_bio_clinical_trial; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient
    ADD CONSTRAINT bio_patient_bio_clinical_trial FOREIGN KEY (bio_experiment_id) REFERENCES bio_clinical_trial(bio_experiment_id);

--
-- Name: bio_patient_bio_subject_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_patient
    ADD CONSTRAINT bio_patient_bio_subject_fk FOREIGN KEY (bio_patient_id) REFERENCES bio_subject(bio_subject_id);

