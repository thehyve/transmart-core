--
-- Name: study_concept_patient_set_bitset; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.study_concept_patient_set_bitset AS
SELECT
  psb.result_instance_id,
  scs.study_id,
  scs.concept_cd,
  -- less efficient length(replace((bitset_result)::text, '0', '')) could be used instead pg_bitcount ext. function.
  public.pg_bitcount(scs.patient_set_bits & psb.patient_set) as patient_count
FROM biomart_user.study_concept_bitset scs, biomart_user.patient_set_bitset psb;
