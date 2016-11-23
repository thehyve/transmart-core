--
-- Name: cz_job_error; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_job_error (
    job_id bigint NOT NULL,
    error_number character varying(300), --number(18,0) in oracle
    error_message character varying(1000),
    error_stack character varying(2000),
    seq_id bigint NOT NULL,
    error_backtrace character varying(2000)
);

