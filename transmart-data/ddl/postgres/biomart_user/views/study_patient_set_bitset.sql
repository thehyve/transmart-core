--
-- Name: study_patient_set_bitset; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.study_patient_set_bitset AS
WITH
  study_bitset AS (
      SELECT study_id, bit_or(patient_set_bits) as patient_set_bits
      FROM biomart_user.study_concept_bitset
      GROUP BY study_id
  )
SELECT
  psb.result_instance_id,
  sb.study_id,
  -- less efficient length(replace((bitset_result)::text, '0', '')) could be used instead pg_bitcount ext. function.
  public.pg_bitcount(sb.patient_set_bits & psb.patient_set) as patient_count
FROM study_bitset sb, biomart_user.patient_set_bitset psb;
