--
-- Type: VIEW; Owner: TM_CZ; Name: ALL_TEST_DETAIL_VIEW
--
  CREATE OR REPLACE FORCE VIEW "TM_CZ"."ALL_TEST_DETAIL_VIEW" ("RUN_ID", "TEST_ID", "RUN_NAME", "TABLE", "COLUMN", "CATEGORY", "SUB_CATEGORY1", "SUB_CATEGORY2", "SQL", "START_DATE", "END_DATE", "STATUS", "TEST_SEVERITY_CD", "ACT_RECORD_CNT", "MIN_VALUE_ALLOWED", "MAX_VALUE_ALLOWED", "ERROR_CODE", "ERROR_MESSAGE") AS 
  select
  a.test_run_id RUN_ID,
  c.test_id,
  a.test_run_name RUN_NAME,
  c.test_table "TABLE",
  c.test_column "COLUMN",
  e.test_category CATEGORY,
  e.test_sub_category1 SUB_CATEGORY1,
  e.test_sub_category2 SUB_CATEGORY2,
  c.test_sql "SQL",
  to_char(b.start_date,'DD/MM/YYYY HH24:MI:SS') start_date,
  to_char(b.end_date,'DD/MM/YYYY HH24:MI:SS') end_date,
  b.status,
  c.test_severity_cd,
  d.act_record_cnt,
  c.test_min_value MIN_VALUE_ALLOWED,
  c.test_max_value MAX_VALUE_ALLOWED,
  d.return_code error_code,
  d.return_message error_message
from az_test_run a
left outer join az_test_step_run b
  on a.test_run_id = b.test_run_id
join cz_test c
  on c.test_id = b.test_id
left outer join az_test_step_act_result d
  on d.test_step_run_id = b.test_step_run_id
join cz_test_category e
  on e.test_category_id = c.test_category_id
ORDER BY a.TEST_RUN_ID, c.test_table, c.test_column
 
 
 
 
;
 
