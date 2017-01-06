ALTER TABLE ONLY i2b2demodata.observation_fact DROP CONSTRAINT observation_fact_pkey;
ALTER TABLE ONLY i2b2demodata.observation_fact ADD CONSTRAINT observation_fact_pkey PRIMARY KEY (encounter_num, patient_num, concept_cd, provider_id, instance_num, modifier_cd, start_date);
ALTER TABLE ONLY i2b2demodata.observation_fact ADD COLUMN trial_visit_num numeric(38,0);
ALTER TABLE ONLY i2b2demodata.observation_fact ADD CONSTRAINT observation_fact_trial_visit_fk FOREIGN KEY (trial_visit_num) REFERENCES i2b2demodata.trial_visit_dimension(trial_visit_num);
CREATE INDEX idx_fact_trial_visit_num ON i2b2demodata.observation_fact USING btree (trial_visit_num);