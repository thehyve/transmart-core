--
-- Name: patient_trial; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE patient_trial (
    patient_num numeric NOT NULL,
    trial character varying(30) NOT NULL,
    secure_obj_token character varying(50)
);

--
-- Name: patient_trial_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY patient_trial
    ADD CONSTRAINT patient_trial_pk PRIMARY KEY (patient_num, trial);

--
-- Name: patient_trial_patient_num_fk; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY patient_trial
    ADD CONSTRAINT patient_trial_patient_num_fk FOREIGN KEY (patient_num) REFERENCES patient_dimension(patient_num) ON DELETE CASCADE;

