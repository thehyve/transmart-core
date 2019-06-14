--
-- Name: patient_set_bitset; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE OR REPLACE VIEW biomart_user.patient_set_bitset AS
SELECT
  qri.result_instance_id AS result_instance_id,
  public.pg_int_to_bit_agg((collection.patient_num - patient_num_boundaries.min_patient_num)::int, patient_num_boundaries.diameter::int) AS patient_set
FROM biomart_user.patient_num_boundaries, i2b2demodata.qt_query_result_instance qri
LEFT OUTER JOIN i2b2demodata.qt_patient_set_collection collection ON collection.result_instance_id = qri.result_instance_id
GROUP BY qri.result_instance_id;
