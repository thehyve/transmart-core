--
-- Name: status_of_export_job; Type: TABLE; Schema: galaxy; Owner: -
--
CREATE TABLE status_of_export_job (
    job_status character varying(200),
    last_export_name character varying(200),
    last_export_time date,
    job_name_id character varying(200),
    id bigint NOT NULL
);

--
-- Name: status_of_export_job_pk; Type: CONSTRAINT; Schema: galaxy; Owner: -
--
ALTER TABLE ONLY status_of_export_job
    ADD CONSTRAINT status_of_export_job_pk PRIMARY KEY (id);

