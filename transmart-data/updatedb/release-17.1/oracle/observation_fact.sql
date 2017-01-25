ALTER TABLE i2b2demodata.observation_fact DROP CONSTRAINT observation_fact_pkey;
UPDATE i2b2demodata.observation_fact SET start_date = TO_DATE('01-01-01', 'yy-mm-dd') WHERE start_date is null;
UPDATE i2b2demodata.observation_fact SET instance_num = 1 WHERE instance_num is null;
ALTER TABLE i2b2demodata.observation_fact ADD CONSTRAINT observation_fact_pkey PRIMARY KEY (encounter_num, patient_num, concept_cd, provider_id, instance_num, modifier_cd, start_date);
ALTER TABLE i2b2demodata.observation_fact ADD trial_visit_num numeric(38,0);
ALTER TABLE i2b2demodata.observation_fact ADD CONSTRAINT trial_visit_dim_fk FOREIGN KEY (trial_visit_num) REFERENCES i2b2demodata.trial_visit_dimension(trial_visit_num);
CREATE INDEX idx_fact_trial_visit_num ON i2b2demodata.observation_fact(trial_visit_num);