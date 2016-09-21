--
-- Name: async_job; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE async_job (
    id integer NOT NULL,
    job_name character varying(200),
    job_status character varying(200),
    run_time character varying(200),
    job_status_time timestamp(6) without time zone,
    last_run_on timestamp(6) without time zone,
    viewer_url character varying(4000),
    alt_viewer_url character varying(4000),
    job_results text,
    job_inputs_json text,
    job_type character varying(20)
);

--
-- Name: async_job_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY async_job
    ADD CONSTRAINT async_job_pkey PRIMARY KEY (id);

