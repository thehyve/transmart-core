-- Change type of encounter_num to serial
alter table i2b2demodata.visit_dimension
  alter column encounter_num type integer;

create sequence seq_patient_num increment by 1 no minvalue no maxvalue cache 1;

alter table i2b2demodata.visit_dimension
  alter column encounter_num set default nextval('seq_patient_num');
update i2b2demodata.visit_dimension set encounter_num = default;

alter table i2b2demodata.observation_fact
  alter column encounter_num type integer;

alter table i2b2demodata.encounter_mapping
  alter column encounter_num type integer;

-- Replace primary key of the visit_dimension table
alter table i2b2demodata.visit_dimension
  drop constraint visit_dimension_pk;

alter table i2b2demodata.visit_dimension
  add constraint visit_dimension_pk primary key (encounter_num);

-- Add foreign keys to visit_dimension en encounter_mapping tables
alter table i2b2demodata.visit_dimension
  add constraint visit_dimension_patient_num_fk foreign key (patient_num)
  references i2b2demodata.patient_dimension(patient_num);

alter table i2b2demodata.encounter_mapping
  add constraint encounter_mapping_encounter_num_fk foreign key (encounter_num)
  references i2b2demodata.visit_dimension(encounter_num);

-- Update documentation
comment on column i2b2demodata.visit_dimension.patient_num is 'Foreign key. Id linking to patient_num in the patient_dimension.';
