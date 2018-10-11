--
-- Name: patient_num_boundaries; Type: VIEW; Schema: biomart_user; Owner: -
--
create or replace view biomart_user.patient_num_boundaries as
select
  min(patient_num) as min_patient_num,
  max(patient_num) as max_patient_num,
  max(patient_num) - min(patient_num) + 1 as diameter
from i2b2demodata.patient_dimension;
