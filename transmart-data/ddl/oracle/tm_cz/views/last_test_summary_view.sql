--
-- Type: VIEW; Owner: TM_CZ; Name: LAST_TEST_SUMMARY_VIEW
--
  CREATE OR REPLACE FORCE VIEW "TM_CZ"."LAST_TEST_SUMMARY_VIEW" ("TEST_RUN_ID", "TEST_RUN_NAME", "START_DATE", "END_DATE", "STATUS", "TEST_CATEGORY", "TEST_SUB_CATEGORY1", "TEST_SUB_CATEGORY2", "PASS", "WARNING", "FAIL", "ERROR", "TOTAL", "DB_VERSION") AS 
  select
  a.test_run_id,
  a.test_run_name,
  to_char(a.start_date,'DD/MM/YYYY HH24:MI:SS') start_date,
  to_char(a.end_date,'DD/MM/YYYY HH24:MI:SS') end_date,
  a.status,
  d.test_category,
  d.test_sub_category1,
  D.test_sub_category2,
  sum(case when b.status = 'PASS' then 1
    else 0 end) PASS,
  sum(case when b.status = 'WARNING' then 1
    else 0 end) WARNING,
  sum(case when b.status = 'FAIL' then 1
    else 0 end) FAIL,
  sum(case when b.status = 'ERROR' then 1
    else 0 end) ERROR,
  count(b.status) TOTAL,
  c.version_name as DB_Version
from az_test_run a
join az_test_step_run b
  on a.test_run_id = b.test_run_id
JOIN cz_dw_version C
  ON c.dw_version_id = a.dw_version_id
JOIN cz_test_category d
  ON d.test_category_id = a.test_category_id
WHERE a.test_run_id = (select max(test_run_id) from az_test_run)
group by
  a.test_run_id,
  a.test_run_name,
  to_char(a.start_date,'DD/MM/YYYY HH24:MI:SS'),
  to_char(a.end_date,'DD/MM/YYYY HH24:MI:SS'),
  a.status,
  d.test_category,
  d.test_sub_category1,
  d.test_sub_category2,
  c.version_name
 
 
 
 
;
 
