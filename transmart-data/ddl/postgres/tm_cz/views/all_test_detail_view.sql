--
-- Name: all_test_detail_view; Type: VIEW; Schema: tm_cz; Owner: -
--
CREATE VIEW all_test_detail_view AS
    SELECT a.test_run_id AS run_id, c.test_id, a.test_run_name AS run_name, c.test_table AS vtable, c.test_column AS vcolumn, e.test_category AS category, e.test_sub_category1 AS sub_category1, e.test_sub_category2 AS sub_category2, c.test_sql AS vsql, to_char(b.start_date, 'DD/MM/YYYY HH24:MI:SS'::text) AS start_date, to_char(b.end_date, 'DD/MM/YYYY HH24:MI:SS'::text) AS end_date, b.status, c.test_severity_cd, d.act_record_cnt, c.test_min_value AS min_value_allowed, c.test_max_value AS max_value_allowed, d.return_code AS error_code, d.return_message AS error_message FROM ((((az_test_run a LEFT JOIN az_test_step_run b ON ((a.test_run_id = b.test_run_id))) JOIN cz_test c ON ((c.test_id = b.test_id))) LEFT JOIN az_test_step_act_result d ON ((d.test_step_run_id = b.test_step_run_id))) JOIN cz_test_category e ON ((e.test_category_id = c.test_category_id))) ORDER BY a.test_run_id, c.test_table, c.test_column;

