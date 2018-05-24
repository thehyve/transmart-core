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
  bitcat(biomart_user.scale_bitset((boundaries.max_patient_num - boundaries.min_patient_num + 1)::bigint), B'1') AS one
FROM boundaries;