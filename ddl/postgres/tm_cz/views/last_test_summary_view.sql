--
-- Name: last_test_summary_view; Type: VIEW; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE VIEW last_test_summary_view (test_run_id, test_run_name, start_date, end_date, status, test_category, test_sub_category1, test_sub_category2, pass, warning, fail, error, total, db_version) AS SELECT 
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
 FROM az_test_run a
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
