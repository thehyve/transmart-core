<?php
$dependencies = array (
  'views/modifier_dimension_view' => 
  array (
    0 => 'modifier_dimension',
    1 => 'modifier_metadata',
  ),
  'concept_counts' => 
  array (
    0 => 'concept_dimension',
  ),
  'set_upload_status' => 
  array (
    0 => 'set_type',
  ),
  'modifier_metadata' => 
  array (
    0 => 'modifier_dimension',
  ),
  'observation_fact' => 
  array (
    0 => 'concept_dimension',
    1 => 'modifier_dimension',
    2 => 'patient_dimension',
  ),
  'patient_trial' => 
  array (
    0 => 'patient_dimension',
  ),
  'qt_patient_enc_collection' => 
  array (
    0 => 'qt_query_result_instance',
  ),
  'qt_patient_set_collection' => 
  array (
    0 => 'qt_query_result_instance',
  ),
  'qt_query_instance' => 
  array (
    0 => 'qt_query_master',
    1 => 'qt_query_status_type',
  ),
  'qt_query_result_instance' => 
  array (
    0 => 'qt_query_instance',
    1 => 'qt_query_result_type',
    2 => 'qt_query_status_type',
  ),
  'qt_xml_result' => 
  array (
    0 => 'qt_query_result_instance',
  ),
)
;