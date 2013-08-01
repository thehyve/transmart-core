--
-- Name: ont_sq_ps_prid; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE ont_sq_ps_prid
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: ont_process_status; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE ont_process_status (
    process_id numeric(5,0) DEFAULT nextval('ont_sq_ps_prid'::regclass) NOT NULL,
    process_type_cd character varying(50),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    process_step_cd character varying(50),
    process_status_cd character varying(50),
    crc_upload_id numeric(38,0),
    status_cd character varying(50),
    message character varying(2000),
    entry_date timestamp without time zone,
    change_date timestamp without time zone,
    changedby_char character(50)
);

--
-- Name: ont_process_status_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY ont_process_status
    ADD CONSTRAINT ont_process_status_pkey PRIMARY KEY (process_id);

