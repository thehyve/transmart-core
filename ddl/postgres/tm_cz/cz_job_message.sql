--
-- Name: cz_job_message; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_job_message (
    job_id bigint NOT NULL,
    message_id bigint,
    message_line bigint,
    message_procedure character varying(100),
    info_message character varying(2000),
    seq_id bigint NOT NULL
);

