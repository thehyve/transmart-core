--
-- Name: az_test_comparison_info; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE az_test_comparison_info (
    test_id bigint,
    param1 character varying(4000),
    prev_dw_version_id bigint,
    prev_test_step_run_id bigint,
    prev_act_record_cnt double precision,
    curr_dw_version_id bigint,
    curr_test_step_run_id bigint,
    curr_act_record_cnt double precision,
    curr_run_date timestamp without time zone,
    prev_run_date date
);

