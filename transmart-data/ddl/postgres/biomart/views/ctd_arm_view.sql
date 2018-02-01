--
-- Name: ctd_arm_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_arm_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.arm, v.arm_nbr_of_patients_studied) AS id,
    v.ref_article_protocol_id,
    v.arm,
    v.arm_nbr_of_patients_studied,
    v.arm_classification_type,
    v.arm_classification_value,
    v.arm_asthma_duration,
    v.arm_geographic_region,
    v.arm_age,
    v.arm_gender,
    v.arm_smokers_pct,
    v.arm_former_smokers_pct,
    v.arm_never_smokers_pct,
    v.arm_pack_years,
    v.minority_participation,
    v.baseline_symptom_score,
    v.baseline_rescue_medication_use
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.arm,
            to_number((ctd_full.arm_nbr_of_patients_studied)::text, '999999999999999'::text) AS arm_nbr_of_patients_studied,
            ctd_full.arm_classification_type,
            ctd_full.arm_classification_value,
            ctd_full.arm_asthma_duration,
            ctd_full.arm_geographic_region,
            ctd_full.arm_age,
            ctd_full.arm_gender,
            ctd_full.arm_smokers_pct,
            ctd_full.arm_former_smokers_pct,
            ctd_full.arm_never_smokers_pct,
            ctd_full.arm_pack_years,
            ctd_full.minority_participation,
            ctd_full.baseline_symptom_score,
            ctd_full.baseline_rescue_medication_use
           FROM ctd_full
          WHERE ((ctd_full.arm IS NOT NULL) AND ((ctd_full.arm)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.arm, (to_number((ctd_full.arm_nbr_of_patients_studied)::text, '999999999999999'::text))) v;

