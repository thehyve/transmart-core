--
-- Name: patient_num_boundaries; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE OR REPLACE VIEW biomart_user.patient_num_boundaries AS
WITH boundaries AS (
SELECT MIN(patient_num) AS min_patient_num, MAX(patient_num) as max_patient_num FROM i2b2demodata.patient_dimension
),
boundaries_with_scaled_bitset AS (
SELECT *, biomart_user.scale_bitset((boundaries.max_patient_num - boundaries.min_patient_num + 1)::bigint) AS s_bitset FROM boundaries
)
SELECT
  boundaries_with_scaled_bitset.min_patient_num,
  boundaries_with_scaled_bitset.max_patient_num,
  bitcat(boundaries_with_scaled_bitset.s_bitset, B'1') AS one,
  bitcat(boundaries_with_scaled_bitset.s_bitset, B'0') as zero
FROM boundaries_with_scaled_bitset;