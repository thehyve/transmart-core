--
-- Name: last_test_detail_view; Type: VIEW; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE VIEW last_test_detail_view (run_id, test_id, run_name, table, column, category, sub_category1, sub_category2, sql, start_date, end_date, status, test_severity_cd, act_record_cnt, min_value_allowed, max_value_allowed, error_code, error_message) AS SELECT 
  a.test_run_id RUN_ID,
  c.test_id,
  a.test_run_name RUN_NAME,
  c.test_table "table",
  c.test_column "column",
  e.test_category CATEGORY,
  e.test_sub_category1 SUB_CATEGORY1,
  e.test_sub_category2 SUB_CATEGORY2,
  c.test_sql "sql",
  to_char(b.start_date,'DD/MM/YYYY HH24:MI:SS') start_date,
  to_char(b.end_date,'DD/MM/YYYY HH24:MI:SS') end_date,
  b.status,
  c.test_severity_cd,
  d.act_record_cnt,
  c.test_min_value MIN_VALUE_ALLOWED,
  c.test_max_value MAX_VALUE_ALLOWED,
  d.return_code error_code,
  d.return_message error_message
 FROM az_test_run a
left outer join az_test_step_run b
  on a.test_run_id = b.test_run_id
join cz_test c
  on c.test_id = b.test_id
left outer join az_test_step_act_result d
  on d.test_step_run_id = b.test_step_run_id
join cz_test_category e
  on e.test_category_id = c.test_category_id
WHERE A.TEST_RUN_ID = (select max(test_Run_id) from az_test_run)
ORDER BY a.TEST_RUN_ID, c.test_table, c.test_column
;
