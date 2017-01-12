--
-- Name: patient_trial; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE patient_trial (
    patient_num numeric,
    trial character varying(30),
    secure_obj_token character varying(50)
);


--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.patient_trial IS 'Table that links patients to trials';

COMMENT ON COLUMN patient_trial.patient_num IS 'Id that links to the patient_num of the patient_dimension table';
COMMENT ON COLUMN patient_trial.trial IS 'Name of the trail.';
COMMENT ON COLUMN patient_trial.secure_obj_token IS 'Token needed for access to the study. E.g., PUBLIC or EXP:GSE8581.';