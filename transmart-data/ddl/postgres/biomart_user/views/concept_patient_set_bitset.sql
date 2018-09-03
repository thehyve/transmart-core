--
-- Name: concept_patient_set_bitset; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.concept_patient_set_bitset AS
WITH
  concept_bitset AS (
      SELECT concept_cd, bit_or(patient_set_bits) as patient_set_bits
      FROM biomart_user.study_concept_bitset
      GROUP BY concept_cd
  )
SELECT
  psb.result_instance_id,
  cb.concept_cd,
  -- less efficient length(replace((bitset_result)::text, '0', '')) could be used instead pg_bitcount ext. function.
  public.pg_bitcount(cb.patient_set_bits & psb.patient_set) as patient_count
FROM concept_bitset cb, biomart_user.patient_set_bitset psb;
