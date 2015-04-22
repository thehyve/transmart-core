<?php
$dependencies = array (
  'de_rc_snp_info1' => 
  array (
    0 => 'de_rc_snp_info',
  ),
  'views/de_variant_summary_detail_gene' => 
  array (
    0 => 'de_variant_population_data',
    1 => 'de_variant_subject_detail',
    2 => 'de_variant_subject_summary',
  ),
  'de_chromosomal_region' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_metabolite_sub_pway_metab' => 
  array (
    0 => 'de_metabolite_annotation',
    1 => 'de_metabolite_sub_pathways',
  ),
  'de_metabolite_sub_pathways' => 
  array (
    0 => 'de_metabolite_super_pathways',
    1 => 'de_gpl_info',
  ),
  'de_metabolite_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_metabolite_super_pathways' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_mrna_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_protein_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_qpcr_mirna_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_rbm_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_rbm_data_annotation_join' => 
  array (
    0 => 'de_rbm_annotation',
    1 => 'de_subject_rbm_data',
  ),
  'de_rnaseq_annotation' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_subject_metabolomics_data' => 
  array (
    0 => 'de_metabolite_annotation',
    1 => 'de_subject_sample_mapping',
  ),
  'de_subject_acgh_data' => 
  array (
    0 => 'de_chromosomal_region',
    1 => 'de_subject_sample_mapping',
  ),
  'de_subject_rnaseq_data' => 
  array (
    0 => 'de_chromosomal_region',
    1 => 'de_subject_sample_mapping',
  ),
  'de_subject_microarray_data' => 
  array (
    0 => 'de_subject_sample_mapping',
    1 => 'de_mrna_annotation',
  ),
  'de_subject_mirna_data' => 
  array (
    0 => 'de_subject_sample_mapping',
    1 => 'de_qpcr_mirna_annotation',
  ),
  'de_subject_protein_data' => 
  array (
    0 => 'de_subject_sample_mapping',
    1 => 'de_protein_annotation',
  ),
  'de_subject_rbm_data' => 
  array (
    0 => 'de_subject_sample_mapping',
  ),
  'de_subject_rna_data' => 
  array (
    0 => 'de_subject_sample_mapping',
    1 => 'de_rnaseq_annotation',
  ),
  'de_subject_sample_mapping' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_variant_population_data' => 
  array (
    0 => 'de_variant_dataset',
  ),
  'de_variant_population_info' => 
  array (
    0 => 'de_variant_dataset',
  ),
  'de_variant_subject_summary' => 
  array (
    0 => 'de_subject_sample_mapping',
    1 => 'de_variant_dataset',
  ),
  'de_xtrial_child_map' => 
  array (
    0 => 'de_xtrial_parent_names',
  ),
  'de_snp_data_by_probe' => 
  array (
    0 => 'de_snp_probe',
    1 => 'de_snp_info',
  ),
  'de_snp_data_by_patient' => 
  array (
    0 => 'de_subject_snp_dataset',
  ),
  'de_snp_gene_map' => 
  array (
    0 => 'de_snp_info',
  ),
  'de_snp_data_dataset_loc' => 
  array (
    0 => 'de_subject_snp_dataset',
  ),
  'de_snp_probe' => 
  array (
    0 => 'de_snp_info',
  ),
  'de_variant_dataset' => 
  array (
    0 => 'de_gpl_info',
  ),
  'de_two_region_event_gene' => 
  array (
    0 => 'de_two_region_event',
  ),
  'de_two_region_junction_event' => 
  array (
    0 => 'de_two_region_event',
    1 => 'de_two_region_junction',
  ),
  'de_variant_metadata' => 
  array (
    0 => 'de_variant_dataset',
  ),
  'de_variant_subject_detail' => 
  array (
    0 => 'de_variant_dataset',
  ),
  'de_variant_subject_idx' => 
  array (
    0 => 'de_variant_dataset',
  ),
)
;