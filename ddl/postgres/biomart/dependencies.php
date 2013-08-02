<?php
$dependencies = array (
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