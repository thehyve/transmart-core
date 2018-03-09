--
-- Name: trial_visit_num_seq; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE trial_visit_num_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

--
-- Name: trial_visit_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE trial_visit_dimension (
    trial_visit_num numeric(38,0) NOT NULL,
    study_num numeric(38,0) NOT NULL,
    rel_time_unit_cd character varying(50),
    rel_time_num numeric(38,0),
    rel_time_label character varying(900)
);

--
-- Name: study_num; Type: DEFAULT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY trial_visit_dimension ALTER COLUMN trial_visit_num SET DEFAULT nextval('trial_visit_num_seq'::regclass);

--
-- Name: trial_visit_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY trial_visit_dimension
    ADD CONSTRAINT trial_visit_dimension_pk PRIMARY KEY (trial_visit_num);

--
-- Name: trial_visit_dimension_study_fk; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY trial_visit_dimension
ADD CONSTRAINT trial_visit_dimension_study_fk FOREIGN KEY (study_num) REFERENCES study(study_num);

--
-- Name: idx_trial_visit_study_num; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX idx_trial_visit_study_num ON trial_visit_dimension USING btree (study_num);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.trial_visit_dimension IS 'Table holds descriptions of trial visits, which represent relative time points in a study.';

COMMENT ON COLUMN trial_visit_dimension.trial_visit_num IS 'Primary key.';
COMMENT ON COLUMN trial_visit_dimension.study_num IS 'Foreign key to study_num in study.';
COMMENT ON COLUMN trial_visit_dimension.rel_time_unit_cd IS 'The unit in which rel_time_num is expressed. E.g., Week, Day, Visit.';
COMMENT ON COLUMN trial_visit_dimension.rel_time_num IS 'E.g., 1 (for Week 1).';
COMMENT ON COLUMN trial_visit_dimension.rel_time_label IS 'Descriptive name. E.g., Baseline, Week 1.';
