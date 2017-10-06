--
-- Name: study_num_seq; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE study_num_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

--
-- Name: study; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE study (
    study_num numeric(38,0) NOT NULL,
    bio_experiment_id bigint,
    study_id character varying(100) NOT NULL,
    secure_obj_token character varying(200) NOT NULL,
    study_blob text
);

--
-- Name: study_num; Type: DEFAULT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study ALTER COLUMN study_num SET DEFAULT nextval('study_num_seq'::regclass);

--
-- Name: study_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY study
    ADD CONSTRAINT study_pk PRIMARY KEY (study_num);

--
-- Name: idx_study_pk; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE UNIQUE INDEX idx_study_pk ON study USING btree (study_num);

--
-- Name: idx_study_id; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE UNIQUE INDEX idx_study_id ON study USING btree (study_id);

--
-- Name: idx_study_secure_obj_token; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX idx_study_secure_obj_token ON study USING btree (secure_obj_token);

--
-- Name: study_bio_experiment_id_fk; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY study
ADD CONSTRAINT study_bio_experiment_id_fk FOREIGN KEY (bio_experiment_id) REFERENCES biomart.bio_experiment(bio_experiment_id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.study IS 'Table holds studies and their access tokens';

COMMENT ON COLUMN study.study_num IS 'Primary key.';
COMMENT ON COLUMN study.bio_experiment_id IS 'Foreign key: bio_experiment_id in bio_experiment.';
COMMENT ON COLUMN study.study_id IS 'E.g., GSE8581.';
COMMENT ON COLUMN study.secure_obj_token IS 'Token needed for access to the study. E.g., ‘PUBLIC’ or ‘EXP:GSE8581’.';
COMMENT ON COLUMN study.study_blob IS 'Stores arbitrary information about the study';
