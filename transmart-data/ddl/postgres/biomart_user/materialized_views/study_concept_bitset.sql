--
-- Name: study_concept_bitset; Type: MATERIALIZED VIEW; Schema: biomart_user; Owner: -
--
CREATE MATERIALIZED VIEW biomart_user.study_concept_bitset AS
SELECT
  s.study_id AS study_id,
  o.concept_cd AS concept_cd,
  bit_or(patient_num_boundaries.one << (o.patient_num - patient_num_boundaries.min_patient_num)::INTEGER) AS patient_set_bits
FROM biomart_user.patient_num_boundaries, i2b2demodata.observation_fact o
JOIN i2b2demodata.trial_visit_dimension tv ON o.trial_visit_num = tv.trial_visit_num
JOIN i2b2demodata.study s ON s.study_num = tv.study_num
WHERE o.modifier_cd = '@'
GROUP BY s.study_id, o.concept_cd;

ALTER TABLE biomart_user.study_concept_bitset
  OWNER TO biomart_user;
GRANT ALL ON TABLE biomart_user.study_concept_bitset TO biomart_user;
GRANT ALL ON TABLE biomart_user.study_concept_bitset TO tm_cz;
