--
-- Name: patient_num_boundaries; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.patient_num_boundaries AS
WITH boundaries AS (
SELECT MIN(patient_num) AS min_patient_num, MAX(patient_num) as max_patient_num FROM i2b2demodata.patient_dimension
)
SELECT
  boundaries.min_patient_num,
  boundaries.max_patient_num,
  lpad('1', (boundaries.max_patient_num - boundaries.min_patient_num + 1)::integer, '0')::bit varying AS one
FROM boundaries;