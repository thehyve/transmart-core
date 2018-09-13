-- Index on observation_fact that includes the primary key and the trial_visit_num column.
create index observation_fact_tmceppis_idx on i2b2demodata.observation_fact using btree (trial_visit_num, modifier_cd, concept_cd, encounter_num, patient_num, provider_id, instance_num, start_date);
