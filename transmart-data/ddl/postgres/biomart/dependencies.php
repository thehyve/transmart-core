<?php
$dependencies = array (
  'views/bio_lit_int_model_view' => 
  array (
    0 => 'bio_lit_int_data',
    1 => 'bio_lit_model_data',
  ),
  'views/bio_marker_correl_mv' => 
  array (
    0 => 'bio_data_correl_descr',
    1 => 'bio_data_correlation',
    2 => 'bio_marker',
  ),
  'views/bio_marker_correl_view' => 
  array (
    0 => 'bio_data_correl_descr',
    1 => 'bio_data_correlation',
    2 => 'bio_marker',
  ),
  'views/bio_marker_exp_analysis_mv' => 
  array (
    0 => 'bio_assay_analysis_data',
    1 => 'bio_assay_data_annotation',
    2 => 'bio_experiment',
    3 => 'bio_marker',
  ),
  'views/ctd_arm_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_biomarker_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_cell_info_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_clinical_chars_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_drug_effects_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_drug_inhibitor_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_events_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_experiments_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_expr_after_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_expr_baseline_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_expr_bio_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_expr_source_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_full_clinical_endpts_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_full_search_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_primary_endpts_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_prior_med_use_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_pulmonary_path_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_quant_params_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_reference_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_runin_therapies_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_secondary_endpts_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_stats_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_study_details_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_design_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_excl_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_inclusion_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_smoker_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_sponsor_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_td_status_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/ctd_treatment_phases_view' => 
  array (
    0 => 'ctd_full',
  ),
  'views/mesh_with_parent' => 
  array (
    0 => 'mesh',
  ),
  'views/vw_faceted_search' => 
  array (
    0 => 'bio_assay_analysis',
    1 => 'bio_assay_analysis_ext',
    2 => 'bio_assay_platform',
    3 => 'bio_asy_analysis_pltfm',
    4 => 'bio_data_observation',
    5 => 'bio_data_platform',
    6 => 'bio_experiment',
    7 => 'bio_observation',
  ),
  'views/vw_faceted_search_disease' => 
  array (
  ),
  'bio_assay_analysis_data' => 
  array (
    0 => 'bio_assay_analysis',
    1 => 'bio_experiment',
    2 => 'bio_assay_platform',
    3 => 'bio_assay_feature_group',
  ),
  'bio_assay_analysis_data_tea' => 
  array (
    0 => 'bio_assay_analysis',
    1 => 'bio_experiment',
    2 => 'bio_assay_platform',
    3 => 'bio_assay_feature_group',
  ),
  'bio_assay_analysis' => 
  array (
    0 => 'bio_asy_analysis_pltfm',
  ),
  'bio_assay_sample' => 
  array (
    0 => 'bio_assay',
    1 => 'bio_sample',
  ),
  'bio_assay' => 
  array (
    0 => 'bio_assay_platform',
    1 => 'bio_experiment',
  ),
  'bio_assay_data' => 
  array (
    0 => 'bio_assay_dataset',
    1 => 'bio_experiment',
    2 => 'bio_sample',
  ),
  'bio_assay_data_stats' => 
  array (
    0 => 'bio_assay_feature_group',
    1 => 'bio_experiment',
    2 => 'bio_assay_dataset',
    3 => 'bio_sample',
  ),
  'bio_asy_data_stats_all' => 
  array (
    0 => 'bio_sample',
  ),
  'bio_clinc_trial_time_pt' => 
  array (
    0 => 'bio_clinical_trial',
  ),
  'bio_clinc_trial_pt_group' => 
  array (
    0 => 'bio_clinical_trial',
  ),
  'bio_clinical_trial' => 
  array (
    0 => 'bio_experiment',
  ),
  'bio_clinc_trial_attr' => 
  array (
    0 => 'bio_clinical_trial',
  ),
  'bio_content_reference' => 
  array (
    0 => 'bio_content',
  ),
  'bio_asy_analysis_dataset' => 
  array (
    0 => 'bio_assay_analysis',
    1 => 'bio_assay_dataset',
  ),
  'bio_assay_dataset' => 
  array (
    0 => 'bio_experiment',
  ),
  'bio_data_compound' => 
  array (
    0 => 'bio_compound',
  ),
  'bio_data_disease' => 
  array (
    0 => 'bio_disease',
  ),
  'bio_curated_data' => 
  array (
    0 => 'bio_curation_dataset',
  ),
  'bio_curation_dataset' => 
  array (
    0 => 'bio_asy_analysis_pltfm',
  ),
  'bio_lit_alt_data' => 
  array (
    0 => 'bio_lit_ref_data',
  ),
  'bio_lit_amd_data' => 
  array (
    0 => 'bio_lit_alt_data',
  ),
  'bio_data_literature' => 
  array (
    0 => 'bio_curation_dataset',
  ),
  'bio_lit_inh_data' => 
  array (
    0 => 'bio_lit_ref_data',
  ),
  'bio_lit_int_data' => 
  array (
    0 => 'bio_lit_ref_data',
  ),
  'bio_lit_pe_data' => 
  array (
    0 => 'bio_lit_ref_data',
  ),
  'bio_data_correlation' => 
  array (
    0 => 'bio_data_correl_descr',
  ),
  'bio_patient' => 
  array (
    0 => 'bio_clinc_trial_pt_group',
    1 => 'bio_clinical_trial',
    2 => 'bio_subject',
  ),
  'bio_patient_event_attr' => 
  array (
    0 => 'bio_clinc_trial_attr',
    1 => 'bio_patient_event',
  ),
  'bio_patient_event' => 
  array (
    0 => 'bio_patient',
    1 => 'bio_clinc_trial_time_pt',
  ),
  'bio_sample' => 
  array (
    0 => 'bio_subject',
    1 => 'bio_cell_line',
    2 => 'bio_patient_event',
  ),
  'bio_data_taxonomy' => 
  array (
    0 => 'bio_taxonomy',
  ),
  'bio_cell_line' => 
  array (
    0 => 'bio_disease',
  ),
  'bio_content' => 
  array (
    0 => 'bio_content_repository',
  ),
)
;