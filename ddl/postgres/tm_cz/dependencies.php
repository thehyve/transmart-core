<?php
$dependencies = array (
  'views/all_test_detail_view' => 
  array (
    0 => 'az_test_run',
    1 => 'az_test_step_act_result',
    2 => 'az_test_step_run',
    3 => 'cz_test',
    4 => 'cz_test_category',
  ),
  'views/all_test_summary_view' => 
  array (
    0 => 'az_test_run',
    1 => 'az_test_step_run',
    2 => 'cz_dw_version',
    3 => 'cz_test_category',
  ),
  'views/last_test_detail_view' => 
  array (
    0 => 'az_test_run',
    1 => 'az_test_step_act_result',
    2 => 'az_test_step_run',
    3 => 'cz_test',
    4 => 'cz_test_category',
  ),
  'views/last_test_summary_view' => 
  array (
    0 => 'az_test_run',
    1 => 'az_test_step_run',
    2 => 'cz_dw_version',
    3 => 'cz_test_category',
  ),
  'az_test_run' => 
  array (
    0 => 'cz_dw_version',
    1 => 'cz_test_category',
  ),
  'az_test_step_act_result' => 
  array (
    0 => 'az_test_step_run',
  ),
  'az_test_step_run' => 
  array (
    0 => 'cz_test',
    1 => 'az_test_run',
  ),
  'cz_data_file' => 
  array (
    0 => 'cz_data',
  ),
  'cz_test_category' => 
  array (
    0 => 'cz_person',
  ),
  'cz_test' => 
  array (
    0 => 'cz_test_category',
  ),
)
;