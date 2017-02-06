CREATE SEQUENCE i2b2demodata.trial_visit_num_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE i2b2demodata.trial_visit_dimension (
    trial_visit_num numeric(38,0) NOT NULL,
    study_num numeric(38,0) NOT NULL,
    rel_time_unit_cd character varying(50),
    rel_time_num numeric(38,0),
    rel_time_label character varying(900)
);

ALTER TABLE ONLY i2b2demodata.trial_visit_dimension ALTER COLUMN trial_visit_num SET DEFAULT nextval('i2b2demodata.trial_visit_num_seq'::regclass);

ALTER TABLE ONLY i2b2demodata.trial_visit_dimension
    ADD CONSTRAINT trial_visit_dimension_pk PRIMARY KEY (trial_visit_num);

CREATE UNIQUE INDEX idx_trial_visit_pk ON i2b2demodata.trial_visit_dimension USING btree (trial_visit_num);

CREATE INDEX idx_trial_visit_study_num ON i2b2demodata.trial_visit_dimension USING btree (study_num);

ALTER TABLE ONLY i2b2demodata.trial_visit_dimension
    ADD CONSTRAINT trial_visit_dimension_study_fk FOREIGN KEY (study_num) REFERENCES i2b2demodata.study(study_num);

ALTER SEQUENCE i2b2demodata.trial_visit_num_seq OWNED BY i2b2demodata.trial_visit_dimension.trial_visit_num;

GRANT SELECT ON TABLE i2b2demodata.trial_visit_dimension TO biomart_user;
GRANT ALL ON TABLE i2b2demodata.trial_visit_dimension TO i2b2demodata;
GRANT ALL ON TABLE i2b2demodata.trial_visit_dimension TO tm_cz;