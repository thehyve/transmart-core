-- Replace primary key of the visit_dimension table
drop constraint i2b2demodata.visit_dimension_pk;

alter table i2b2demodata.visit_dimension
  add constraint visit_dimension_pk primary key (encounter_num);

-- Add trigger to generate an encounter_num when inserting into visit_dimension
CREATE SEQUENCE "I2B2DEMODATA"."SEQ_ENCOUNTER_NUM" MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 CACHE 20 NOORDER  NOCYCLE ;

CREATE OR REPLACE TRIGGER "I2B2DEMODATA"."TRG_VISIT_DIMENSION"
before insert on "VISIT_DIMENSION"
for each row
begin
  if inserting then
    if :NEW."ENCOUNTER_NUM" is null then
      select SEQ_ENCOUNTER_NUM.nextval into :NEW."ENCOUNTER_NUM" from dual;
    end if;
  end if;
end;

/
ALTER TRIGGER "I2B2DEMODATA"."TRG_VISIT_DIMENSION" ENABLE;

-- Add foreign keys to visit_dimension en encounter_mapping tables
alter table i2b2demodata.visit_dimension
  add constraint visit_dimension_patient_num_fk foreign key (patient_num)
  references patient_dimension(patient_num);

alter table i2b2demodata.encounter_mapping
  add constraint encounter_mapping_encounter_num_fk foreign key (encounter_num)
  references visit_dimension(encounter_num);

-- Update documentation
comment on column i2b2demodata.visit_dimension.patient_num is 'Foreign key. Id linking to patient_num in the patient_dimension.';
