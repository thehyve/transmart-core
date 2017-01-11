CREATE SEQUENCE i2b2demodata.study_num_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE i2b2demodata.study (
    study_num numeric(38,0) NOT NULL,
    bio_experiment_id bigint,
    study_id character varying(100) NOT NULL,
    secure_obj_token character varying(200) NOT NULL
);

ALTER TABLE ONLY i2b2demodata.study ALTER COLUMN study_num SET DEFAULT nextval('i2b2demodata.study_num_seq'::regclass);

ALTER TABLE ONLY i2b2demodata.study
    ADD CONSTRAINT study_pk PRIMARY KEY (study_num);

CREATE UNIQUE INDEX idx_study_id ON i2b2demodata.study USING btree (study_id);

CREATE UNIQUE INDEX idx_study_pk ON i2b2demodata.study USING btree (study_num);

CREATE INDEX idx_study_secure_obj_token ON i2b2demodata.study USING btree (secure_obj_token);

ALTER SEQUENCE i2b2demodata.study_num_seq OWNED BY i2b2demodata.study.study_num;

ALTER TABLE i2b2demodata.study ADD CONSTRAINT study_bio_experiment_id_fk FOREIGN KEY (bio_experiment_id) REFERENCES biomart.bio_experiment(bio_experiment_id);

GRANT SELECT ON TABLE i2b2demodata.study TO biomart_user;
GRANT ALL ON TABLE i2b2demodata.study TO i2b2demodata;
GRANT ALL ON TABLE i2b2demodata.study TO tm_cz;