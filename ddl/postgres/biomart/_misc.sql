--
-- Name: bio_assay_data_stats_seq; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE bio_assay_data_stats_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: bio_lit_int_model_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_lit_int_model_view AS
 SELECT DISTINCT s.bio_lit_int_data_id, 
    s.experimental_model
   FROM (         SELECT a.bio_lit_int_data_id, 
                    b.experimental_model
                   FROM (bio_lit_int_data a
              JOIN bio_lit_model_data b ON ((a.in_vivo_model_id = b.bio_lit_model_data_id)))
             WHERE (b.experimental_model IS NOT NULL)
        UNION 
                 SELECT a.bio_lit_int_data_id, 
                    b.experimental_model
                   FROM (bio_lit_int_data a
              JOIN bio_lit_model_data b ON ((a.in_vitro_model_id = b.bio_lit_model_data_id)))
             WHERE (b.experimental_model IS NOT NULL)) s;

--
-- Name: bio_marker_correl_mv; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_correl_mv AS
        (        (        (        (         SELECT DISTINCT b.bio_marker_id, 
                                            b.bio_marker_id AS asso_bio_marker_id, 
                                            'GENE'::text AS correl_type, 
                                            1 AS mv_id
                                           FROM bio_marker b
                                          WHERE ((b.bio_marker_type)::text = 'GENE'::text)
                                UNION 
                                         SELECT DISTINCT b.bio_marker_id, 
                                            b.bio_marker_id AS asso_bio_marker_id, 
                                            'Protein'::text AS correl_type, 
                                            4 AS mv_id
                                           FROM bio_marker b
                                          WHERE ((b.bio_marker_type)::text = 'Protein'::text))
                        UNION 
                                 SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                                    c.asso_bio_data_id AS asso_bio_marker_id, 
                                    'PATHWAY_GENE'::text AS correl_type, 
                                    2 AS mv_id
                                   FROM bio_marker b, 
                                    bio_data_correlation c, 
                                    bio_data_correl_descr d
                                  WHERE ((((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((b.primary_source_code)::text <> 'ARIADNE'::text)) AND ((d.correlation)::text = 'PATHWAY GENE'::text)))
                UNION 
                         SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                            c.asso_bio_data_id AS asso_bio_marker_id, 
                            'HOMOLOGENE_GENE'::text AS correl_type, 
                            3 AS mv_id
                           FROM bio_marker b, 
                            bio_data_correlation c, 
                            bio_data_correl_descr d
                          WHERE (((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((d.correlation)::text = 'HOMOLOGENE GENE'::text)))
        UNION 
                 SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                    c.asso_bio_data_id AS asso_bio_marker_id, 
                    'PROTEIN TO GENE'::text AS correl_type, 
                    5 AS mv_id
                   FROM bio_marker b, 
                    bio_data_correlation c, 
                    bio_data_correl_descr d
                  WHERE (((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((d.correlation)::text = 'PROTEIN TO GENE'::text)))
UNION 
         SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
            c.asso_bio_data_id AS asso_bio_marker_id, 
            'GENE TO PROTEIN'::text AS correl_type, 
            6 AS mv_id
           FROM bio_marker b, 
            bio_data_correlation c, 
            bio_data_correl_descr d
          WHERE (((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((d.correlation)::text = 'GENE TO PROTEIN'::text));

--
-- Name: bio_marker_correl_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_correl_view AS
        (         SELECT DISTINCT b.bio_marker_id, 
                    b.bio_marker_id AS asso_bio_marker_id, 
                    'GENE'::text AS correl_type, 
                    1 AS mv_id
                   FROM bio_marker b
                  WHERE ((b.bio_marker_type)::text = 'GENE'::text)
        UNION 
                 SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
                    c.asso_bio_data_id AS asso_bio_marker_id, 
                    'PATHWAY_GENE'::text AS correl_type, 
                    2 AS mv_id
                   FROM bio_marker b, 
                    bio_data_correlation c, 
                    bio_data_correl_descr d
                  WHERE ((((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((b.primary_source_code)::text <> 'ARIADNE'::text)) AND ((d.correlation)::text = 'PATHWAY GENE'::text)))
UNION 
         SELECT DISTINCT c.bio_data_id AS bio_marker_id, 
            c.asso_bio_data_id AS asso_bio_marker_id, 
            'HOMOLOGENE_GENE'::text AS correl_type, 
            3 AS mv_id
           FROM bio_marker b, 
            bio_data_correlation c, 
            bio_data_correl_descr d
          WHERE (((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((d.correlation)::text = 'HOMOLOGENE GENE'::text));

--
-- Name: bio_marker_exp_analysis_mv; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_exp_analysis_mv AS
 SELECT DISTINCT t3.bio_marker_id, 
    t1.bio_experiment_id, 
    t1.bio_assay_analysis_id, 
    ((t1.bio_assay_analysis_id * 100) + t3.bio_marker_id) AS mv_id
   FROM bio_assay_analysis_data t1, 
    bio_experiment t2, 
    bio_marker t3, 
    bio_assay_data_annotation t4
  WHERE ((((t1.bio_experiment_id = t2.bio_experiment_id) AND ((t2.bio_experiment_type)::text = 'Experiment'::text)) AND (t3.bio_marker_id = t4.bio_marker_id)) AND (t1.bio_assay_feature_group_id = t4.bio_assay_feature_group_id));

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
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.arm, to_number((ctd_full.arm_nbr_of_patients_studied)::text, '999999999999999'::text)) v;

--
-- Name: ctd_biomarker_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_biomarker_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomarker_name) AS id, 
    v.ref_article_protocol_id, 
    v.biomarker_name, 
    v.biomarker_pct, 
    v.biomarker_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.biomarker_name, 
            ctd_full.biomarker_pct, 
            ctd_full.biomarker_value
           FROM ctd_full
          WHERE ((ctd_full.biomarker_name IS NOT NULL) AND ((ctd_full.biomarker_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomarker_name) v;

--
-- Name: ctd_cell_info_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_cell_info_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.cellinfo_type) AS id, 
    v.ref_article_protocol_id, 
    v.cellinfo_type, 
    v.cellinfo_count, 
    v.cellinfo_source
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.cellinfo_type, 
            ctd_full.cellinfo_count, 
            ctd_full.cellinfo_source
           FROM ctd_full
          WHERE ((ctd_full.cellinfo_type IS NOT NULL) AND ((ctd_full.cellinfo_type)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.cellinfo_type) v;

--
-- Name: ctd_clinical_chars_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_clinical_chars_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.clinical_variable) AS id, 
    v.ref_article_protocol_id, 
    v.clinical_variable, 
    v.clinical_variable_pct, 
    v.clinical_variable_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.clinical_variable, 
            ctd_full.clinical_variable_pct, 
            ctd_full.clinical_variable_value
           FROM ctd_full
          WHERE ((ctd_full.clinical_variable IS NOT NULL) AND ((ctd_full.clinical_variable)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.clinical_variable) v;

--
-- Name: ctd_drug_effects_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_drug_effects_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.beneficial_effects, v.adverse_effects) AS id, 
    v.ref_article_protocol_id, 
    v.discontinuation_rate, 
    v.response_rate, 
    v.downstream_signaling_effects, 
    v.beneficial_effects, 
    v.adverse_effects, 
    v.pk_pd_parameter, 
    v.pk_pd_value, 
    v.effect_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.discontinuation_rate, 
            ctd_full.response_rate, 
            ctd_full.downstream_signaling_effects, 
            ctd_full.beneficial_effects, 
            ctd_full.adverse_effects, 
            ctd_full.pk_pd_parameter, 
            ctd_full.pk_pd_value, 
            ctd_full.effect_description
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.beneficial_effects, ctd_full.adverse_effects) v;

--
-- Name: ctd_drug_inhibitor_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_drug_inhibitor_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.drug_inhibitor_common_name) AS id, 
    v.ref_article_protocol_id, 
    v.drug_inhibitor_common_name, 
    v.drug_inhibitor_standard_name, 
    v.drug_inhibitor_cas_id
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.drug_inhibitor_common_name, 
            ctd_full.drug_inhibitor_standard_name, 
            ctd_full.drug_inhibitor_cas_id
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name) v;

--
-- Name: ctd_events_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_events_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.definition_of_the_event) AS id, 
    v.ref_article_protocol_id, 
    v.definition_of_the_event, 
    v.number_of_events, 
    v.event_rate, 
    v.time_to_event, 
    v.event_pct_reduction, 
    v.event_p_value, 
    v.event_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.definition_of_the_event, 
            ctd_full.number_of_events, 
            ctd_full.event_rate, 
            ctd_full.time_to_event, 
            ctd_full.event_pct_reduction, 
            ctd_full.event_p_value, 
            ctd_full.event_description
           FROM ctd_full
          WHERE (((ctd_full.definition_of_the_event IS NOT NULL) AND ((ctd_full.definition_of_the_event)::text <> ''::text)) OR ((ctd_full.event_description IS NOT NULL) AND ((ctd_full.event_description)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.definition_of_the_event) v;

--
-- Name: ctd_experiments_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_experiments_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.drug_inhibitor_common_name, v.drug_inhibitor_trtmt_regime) AS id, 
    v.ref_article_protocol_id, 
    v.drug_inhibitor_common_name, 
    v.drug_inhibitor_dose, 
    v.drug_inhibitor_time_period, 
    v.drug_inhibitor_route_of_admin, 
    v.drug_inhibitor_trtmt_regime, 
    v.comparator_name, 
    v.comparator_dose, 
    v.comparator_time_period, 
    v.comparator_route_of_admin, 
    v.treatment_regime, 
    v.placebo, 
    v.experiment_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.drug_inhibitor_common_name, 
            ctd_full.drug_inhibitor_time_period, 
            ctd_full.drug_inhibitor_dose, 
            ctd_full.drug_inhibitor_route_of_admin, 
            ctd_full.drug_inhibitor_trtmt_regime, 
            ctd_full.comparator_name, 
            ctd_full.comparator_dose, 
            ctd_full.comparator_time_period, 
            ctd_full.comparator_route_of_admin, 
            ctd_full.treatment_regime, 
            ctd_full.placebo, 
            ctd_full.experiment_description
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name, ctd_full.drug_inhibitor_trtmt_regime) v;

--
-- Name: ctd_expr_after_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_after_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id, 
    v.ref_article_protocol_id, 
    v.biomolecule_name, 
    v.expr_after_trtmt_pct, 
    v.expr_after_trtmt_number, 
    v.expr_aftertrtmt_valuefold_mean, 
    v.expr_after_trtmt_sd, 
    v.expr_after_trtmt_sem, 
    v.expr_after_trtmt_unit
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.biomolecule_name, 
            ctd_full.expr_after_trtmt_pct, 
            ctd_full.expr_after_trtmt_number, 
            ctd_full.expr_aftertrtmt_valuefold_mean, 
            ctd_full.expr_after_trtmt_sd, 
            ctd_full.expr_after_trtmt_sem, 
            ctd_full.expr_after_trtmt_unit
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.expr_aftertrtmt_valuefold_mean IS NOT NULL) AND ((ctd_full.expr_aftertrtmt_valuefold_mean)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

--
-- Name: ctd_expr_baseline_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_baseline_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id, 
    v.ref_article_protocol_id, 
    v.biomolecule_name, 
    v.baseline_expr_pct, 
    v.baseline_expr_number, 
    v.baseline_expr_value_fold_mean, 
    v.baseline_expr_sd, 
    v.baseline_expr_sem, 
    v.baseline_expr_unit
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.biomolecule_name, 
            ctd_full.baseline_expr_pct, 
            ctd_full.baseline_expr_number, 
            ctd_full.baseline_expr_value_fold_mean, 
            ctd_full.baseline_expr_sd, 
            ctd_full.baseline_expr_sem, 
            ctd_full.baseline_expr_unit
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.baseline_expr_value_fold_mean IS NOT NULL) AND ((ctd_full.baseline_expr_value_fold_mean)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

--
-- Name: ctd_expr_bio_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_bio_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id, 
    v.ref_article_protocol_id, 
    v.biomolecule_name, 
    v.biomolecule_id, 
    v.biomolecule_type, 
    v.biomarker, 
    v.biomarker_type
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.biomolecule_name, 
            ctd_full.biomolecule_id, 
            ctd_full.biomolecule_type, 
            ctd_full.biomarker, 
            ctd_full.biomarker_type
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.biomolecule_id IS NOT NULL) AND ((ctd_full.biomolecule_id)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

--
-- Name: ctd_expr_source_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_source_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.expr_chg_source_type) AS id, 
    v.ref_article_protocol_id, 
    v.expr_chg_source_type, 
    v.expr_chg_technique, 
    v.expr_chg_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.expr_chg_source_type, 
            ctd_full.expr_chg_technique, 
            ctd_full.expr_chg_description
           FROM ctd_full
          WHERE (((ctd_full.expr_chg_source_type IS NOT NULL) AND ((ctd_full.expr_chg_source_type)::text <> ''::text)) OR ((ctd_full.expr_chg_description IS NOT NULL) AND ((ctd_full.expr_chg_description)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.expr_chg_source_type) v;

--
-- Name: ctd_full_clinical_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_full_clinical_endpts_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id, 
    v.ref_article_protocol_id, 
    v.primary_endpoint_type, 
    v.primary_endpoint_definition, 
    v.primary_endpoint_change, 
    v.primary_endpoint_time_period, 
    v.primary_endpoint_p_value, 
    v.primary_endpoint_stat_test, 
    v.secondary_type, 
    v.secondary_type_definition, 
    v.secondary_type_change, 
    v.secondary_type_time_period, 
    v.secondary_type_p_value, 
    v.secondary_type_stat_test
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.primary_endpoint_type, 
            ctd_full.primary_endpoint_definition, 
            ctd_full.primary_endpoint_change, 
            ctd_full.primary_endpoint_time_period, 
            ctd_full.primary_endpoint_p_value, 
            ctd_full.primary_endpoint_stat_test, 
            ctd_full.secondary_type, 
            ctd_full.secondary_type_definition, 
            ctd_full.secondary_type_change, 
            ctd_full.secondary_type_time_period, 
            ctd_full.secondary_type_p_value, 
            ctd_full.secondary_type_stat_test
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id) v;

--
-- Name: ctd_full_search_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_full_search_view AS
 SELECT row_number() OVER (ORDER BY t.ref_article_protocol_id) AS fact_id, 
    t.ref_article_protocol_id, 
    t.mesh, 
    t.common_name, 
    t.drug_inhibitor_standard_name, 
    t.primary_endpoint_type, 
    t.secondary_type, 
    t.biomarker_name, 
    t.disease_severity, 
    t.inhaled_steroid_dose, 
    t.fev1, 
    t.primary_endpoint_time_period, 
    t.primary_endpoint_change, 
    t.primary_endpoint_p_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.mesh, 
            ctd_full.common_name, 
            ctd_full.drug_inhibitor_standard_name, 
            ctd_full.primary_endpoint_type, 
            ctd_full.secondary_type, 
            ctd_full.biomarker_name, 
            ctd_full.disease_severity, 
            ctd_full.inhaled_steroid_dose, 
            ctd_full.fev1, 
            ctd_full.primary_endpoint_time_period, 
            ctd_full.primary_endpoint_change, 
            ctd_full.primary_endpoint_p_value
           FROM ctd_full) t;

--
-- Name: ctd_primary_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_primary_endpts_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.primary_endpoint_type) AS id, 
    v.ref_article_protocol_id, 
    v.primary_endpoint_type, 
    v.primary_endpoint_definition, 
    v.primary_endpoint_change, 
    v.primary_endpoint_time_period, 
    v.primary_endpoint_p_value, 
    v.primary_endpoint_stat_test
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.primary_endpoint_type, 
            ctd_full.primary_endpoint_definition, 
            ctd_full.primary_endpoint_change, 
            ctd_full.primary_endpoint_time_period, 
            ctd_full.primary_endpoint_p_value, 
            ctd_full.primary_endpoint_stat_test
           FROM ctd_full
          WHERE (((ctd_full.primary_endpoint_type IS NOT NULL) AND ((ctd_full.primary_endpoint_type)::text <> ''::text)) OR ((ctd_full.primary_endpoint_definition IS NOT NULL) AND ((ctd_full.primary_endpoint_definition)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.primary_endpoint_type) v;

--
-- Name: ctd_prior_med_use_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_prior_med_use_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.prior_med_drug_name) AS id, 
    v.ref_article_protocol_id, 
    v.prior_med_drug_name, 
    v.prior_med_pct, 
    v.prior_med_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.prior_med_drug_name, 
            ctd_full.prior_med_pct, 
            ctd_full.prior_med_value
           FROM ctd_full
          WHERE ((ctd_full.prior_med_drug_name IS NOT NULL) AND ((ctd_full.prior_med_drug_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.prior_med_drug_name) v;

--
-- Name: ctd_pulmonary_path_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_pulmonary_path_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.pulmonary_pathology_name) AS id, 
    v.ref_article_protocol_id, 
    v.pulmonary_pathology_name, 
    v.pulmpath_patient_pct, 
    v.pulmpath_value_unit, 
    v.pulmpath_method
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.pulmonary_pathology_name, 
            ctd_full.pulmpath_patient_pct, 
            ctd_full.pulmpath_value_unit, 
            ctd_full.pulmpath_method
           FROM ctd_full
          WHERE ((ctd_full.pulmonary_pathology_name IS NOT NULL) AND ((ctd_full.pulmonary_pathology_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.pulmonary_pathology_name) v;

--
-- Name: ctd_quant_params_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_quant_params_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id, 
    v.ref_article_protocol_id, 
    v.clinical_variable_name, 
    v.pct_change_from_baseline, 
    v.abs_change_from_baseline, 
    v.rate_of_change_from_baseline, 
    v.average_over_treatment_period, 
    v.within_group_changes, 
    v.stat_measure_p_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.clinical_variable_name, 
            ctd_full.pct_change_from_baseline, 
            ctd_full.abs_change_from_baseline, 
            ctd_full.rate_of_change_from_baseline, 
            ctd_full.average_over_treatment_period, 
            ctd_full.within_group_changes, 
            ctd_full.stat_measure_p_value
           FROM ctd_full
          WHERE ((ctd_full.clinical_variable_name IS NOT NULL) AND ((ctd_full.clinical_variable_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id) v;

--
-- Name: ctd_reference_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_reference_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.ref_record_id) AS id, 
    v.ref_article_protocol_id, 
    v.ref_article_pmid, 
    v.ref_protocol_id, 
    v.ref_title, 
    v.ref_record_id, 
    v.ref_back_reference
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.ref_article_pmid, 
            ctd_full.ref_protocol_id, 
            ctd_full.ref_title, 
            ctd_full.ref_record_id, 
            ctd_full.ref_back_reference
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.ref_record_id) v;

--
-- Name: ctd_runin_therapies_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_runin_therapies_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.runin_description) AS id, 
    v.ref_article_protocol_id, 
    v.runin_ocs, 
    v.runin_ics, 
    v.runin_laba, 
    v.runin_ltra, 
    v.runin_corticosteroids, 
    v.runin_anti_fibrotics, 
    v.runin_immunosuppressive, 
    v.runin_cytotoxic, 
    v.runin_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.runin_ocs, 
            ctd_full.runin_ics, 
            ctd_full.runin_laba, 
            ctd_full.runin_ltra, 
            ctd_full.runin_corticosteroids, 
            ctd_full.runin_anti_fibrotics, 
            ctd_full.runin_immunosuppressive, 
            ctd_full.runin_cytotoxic, 
            ctd_full.runin_description
           FROM ctd_full
          WHERE ((((ctd_full.runin_ocs IS NOT NULL) AND ((ctd_full.runin_ocs)::text <> ''::text)) OR ((ctd_full.runin_description IS NOT NULL) AND ((ctd_full.runin_description)::text <> ''::text))) OR ((ctd_full.runin_immunosuppressive IS NOT NULL) AND ((ctd_full.runin_immunosuppressive)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.runin_description) v;

--
-- Name: ctd_secondary_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_secondary_endpts_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.secondary_type) AS id, 
    v.ref_article_protocol_id, 
    v.secondary_type, 
    v.secondary_type_definition, 
    v.secondary_type_change, 
    v.secondary_type_time_period, 
    v.secondary_type_p_value, 
    v.secondary_type_stat_test
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.secondary_type, 
            ctd_full.secondary_type_definition, 
            ctd_full.secondary_type_change, 
            ctd_full.secondary_type_time_period, 
            ctd_full.secondary_type_p_value, 
            ctd_full.secondary_type_stat_test
           FROM ctd_full
          WHERE (((ctd_full.secondary_type IS NOT NULL) AND ((ctd_full.secondary_type)::text <> ''::text)) OR ((ctd_full.secondary_type_definition IS NOT NULL) AND ((ctd_full.secondary_type_definition)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.secondary_type) v;

--
-- Name: ctd_stats_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_stats_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.statistical_test) AS id, 
    v.ref_article_protocol_id, 
    v.clinical_correlation, 
    v.statistical_test, 
    v.statistical_coefficient_value, 
    v.statistical_test_p_value, 
    v.statistical_test_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.clinical_correlation, 
            ctd_full.statistical_test, 
            ctd_full.statistical_coefficient_value, 
            ctd_full.statistical_test_p_value, 
            ctd_full.statistical_test_description
           FROM ctd_full
          WHERE (((ctd_full.statistical_test_description IS NOT NULL) AND ((ctd_full.statistical_test_description)::text <> ''::text)) OR ((ctd_full.statistical_test IS NOT NULL) AND ((ctd_full.statistical_test)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.statistical_test) v;

--
-- Name: ctd_study_details_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_study_details_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.common_name) AS id, 
    v.ref_article_protocol_id, 
    v.study_type, 
    v.common_name, 
    v.icd10, 
    v.mesh, 
    v.disease_type, 
    v.physiology_name
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.study_type, 
            ctd_full.common_name, 
            ctd_full.icd10, 
            ctd_full.mesh, 
            ctd_full.disease_type, 
            ctd_full.physiology_name
           FROM ctd_full
          WHERE ((ctd_full.common_name IS NOT NULL) AND ((ctd_full.common_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.common_name) v;

--
-- Name: ctd_td_design_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_design_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.nature_of_trial, v.trial_type) AS id, 
    v.ref_article_protocol_id, 
    v.nature_of_trial, 
    v.randomization, 
    v.blinded_trial, 
    v.trial_type, 
    v.run_in_period, 
    v.treatment_period, 
    v.washout_period, 
    v.open_label_extension
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.nature_of_trial, 
            ctd_full.randomization, 
            ctd_full.blinded_trial, 
            ctd_full.trial_type, 
            ctd_full.run_in_period, 
            ctd_full.treatment_period, 
            ctd_full.washout_period, 
            ctd_full.open_label_extension
           FROM ctd_full
          WHERE (((ctd_full.trial_type IS NOT NULL) AND ((ctd_full.trial_type)::text <> ''::text)) OR ((ctd_full.nature_of_trial IS NOT NULL) AND ((ctd_full.nature_of_trial)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.nature_of_trial, ctd_full.trial_type) v;

--
-- Name: ctd_td_excl_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_excl_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id, 
    v.ref_article_protocol_id, 
    v.exclusion_criteria1, 
    v.exclusion_criteria2, 
    v.minimal_symptoms, 
    v.rescue_medication_use, 
    v.control_details, 
    v.blinding_procedure, 
    v.number_of_arms, 
    v.description1, 
    v.description2
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            "substring"(ctd_full.exclusion_criteria, 1, 4000) AS exclusion_criteria1, 
            "substring"(ctd_full.exclusion_criteria, 4001, 2000) AS exclusion_criteria2, 
            ctd_full.minimal_symptoms, 
            ctd_full.rescue_medication_use, 
            ctd_full.control_details, 
            ctd_full.blinding_procedure, 
            ctd_full.number_of_arms, 
            "substring"(ctd_full.description, 1, 4000) AS description1, 
            "substring"(ctd_full.description, 4001, 2000) AS description2
           FROM ctd_full
          WHERE (((ctd_full.blinding_procedure IS NOT NULL) AND ((ctd_full.blinding_procedure)::text <> ''::text)) OR ((ctd_full.number_of_arms IS NOT NULL) AND ((ctd_full.number_of_arms)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id) v;

--
-- Name: ctd_td_inclusion_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_inclusion_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.disease_severity, v.fev1) AS id, 
    v.ref_article_protocol_id, 
    v.trial_age, 
    v.disease_severity, 
    v.difficult_to_treat, 
    v.asthma_diagnosis, 
    v.inhaled_steroid_dose, 
    v.laba, 
    v.ocs, 
    v.xolair, 
    v.ltra_inhibitors, 
    v.asthma_phenotype, 
    v.fev1, 
    v.fev1_reversibility, 
    v.tlc, 
    v.fev1_fvc, 
    v.fvc, 
    v.dlco, 
    v.sgrq, 
    v.hrct, 
    v.biopsy, 
    v.dyspnea_on_exertion, 
    v.concomitant_med
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.trial_age, 
            ctd_full.disease_severity, 
            ctd_full.difficult_to_treat, 
            ctd_full.asthma_diagnosis, 
            ctd_full.inhaled_steroid_dose, 
            ctd_full.laba, 
            ctd_full.ocs, 
            ctd_full.xolair, 
            ctd_full.ltra_inhibitors, 
            ctd_full.asthma_phenotype, 
            ctd_full.fev1, 
            ctd_full.fev1_reversibility, 
            ctd_full.tlc, 
            ctd_full.fev1_fvc, 
            ctd_full.fvc, 
            ctd_full.dlco, 
            ctd_full.sgrq, 
            ctd_full.hrct, 
            ctd_full.biopsy, 
            ctd_full.dyspnea_on_exertion, 
            ctd_full.concomitant_med
           FROM ctd_full
          WHERE ((((ctd_full.fev1 IS NOT NULL) AND ((ctd_full.fev1)::text <> ''::text)) OR ((ctd_full.disease_severity IS NOT NULL) AND ((ctd_full.disease_severity)::text <> ''::text))) OR ((ctd_full.trial_age IS NOT NULL) AND ((ctd_full.trial_age)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.disease_severity, ctd_full.fev1) v;

--
-- Name: ctd_td_smoker_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_smoker_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.trial_smokers_pct) AS id, 
    v.ref_article_protocol_id, 
    v.trial_smokers_pct, 
    v.trial_former_smokers_pct, 
    v.trial_never_smokers_pct, 
    v.trial_pack_years
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.trial_smokers_pct, 
            ctd_full.trial_former_smokers_pct, 
            ctd_full.trial_never_smokers_pct, 
            ctd_full.trial_pack_years
           FROM ctd_full
          WHERE (((ctd_full.trial_smokers_pct IS NOT NULL) AND ((ctd_full.trial_smokers_pct)::text <> ''::text)) OR ((ctd_full.trial_never_smokers_pct IS NOT NULL) AND ((ctd_full.trial_never_smokers_pct)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.trial_smokers_pct) v;

--
-- Name: ctd_td_sponsor_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_sponsor_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.sponsor, v.trial_nbr_of_patients_studied) AS id, 
    v.ref_article_protocol_id, 
    v.sponsor, 
    v.trial_nbr_of_patients_studied, 
    v.source_type
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.sponsor, 
            ctd_full.trial_nbr_of_patients_studied, 
            ctd_full.source_type
           FROM ctd_full
          WHERE (((ctd_full.sponsor IS NOT NULL) AND ((ctd_full.sponsor)::text <> ''::text)) OR ((ctd_full.trial_nbr_of_patients_studied IS NOT NULL) AND ((ctd_full.trial_nbr_of_patients_studied)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.sponsor, ctd_full.trial_nbr_of_patients_studied) v;

--
-- Name: ctd_td_status_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_status_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id, 
    v.ref_article_protocol_id, 
    v.trial_status, 
    v.trial_phase
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.trial_status, 
            ctd_full.trial_phase
           FROM ctd_full
          WHERE (((ctd_full.trial_status IS NOT NULL) AND ((ctd_full.trial_status)::text <> ''::text)) OR ((ctd_full.trial_phase IS NOT NULL) AND ((ctd_full.trial_phase)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id) v;

--
-- Name: ctd_treatment_phases_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_treatment_phases_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.trtmt_description, v.trtmt_ocs) AS id, 
    v.ref_article_protocol_id, 
    v.trtmt_ocs, 
    v.trtmt_ics, 
    v.trtmt_laba, 
    v.trtmt_ltra, 
    v.trtmt_corticosteroids, 
    v.trtmt_anti_fibrotics, 
    v.trtmt_immunosuppressive, 
    v.trtmt_cytotoxic, 
    v.trtmt_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id, 
            ctd_full.trtmt_ocs, 
            ctd_full.trtmt_ics, 
            ctd_full.trtmt_laba, 
            ctd_full.trtmt_ltra, 
            ctd_full.trtmt_corticosteroids, 
            ctd_full.trtmt_anti_fibrotics, 
            ctd_full.trtmt_immunosuppressive, 
            ctd_full.trtmt_cytotoxic, 
            ctd_full.trtmt_description
           FROM ctd_full
          WHERE ((((ctd_full.trtmt_ocs IS NOT NULL) AND ((ctd_full.trtmt_ocs)::text <> ''::text)) OR ((ctd_full.trtmt_description IS NOT NULL) AND ((ctd_full.trtmt_description)::text <> ''::text))) OR ((ctd_full.trtmt_immunosuppressive IS NOT NULL) AND ((ctd_full.trtmt_immunosuppressive)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.trtmt_description, ctd_full.trtmt_ocs) v;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE hibernate_sequence
    START WITH 226
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

