--
-- Name: qt_sq_qm_qmid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qm_qmid
    START WITH 28756
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_query_master; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_query_master (
    query_master_id numeric(5,0) DEFAULT nextval('qt_sq_qm_qmid'::regclass) NOT NULL,
    name character varying(250) NOT NULL,
    user_id character varying(50) NOT NULL,
    group_id character varying(50) NOT NULL,
    master_type_cd character varying(2000),
    plugin_id numeric(10,0),
    create_date timestamp without time zone NOT NULL,
    delete_date timestamp without time zone,
    delete_flag character varying(3),
    generated_sql text,
    request_xml text,
    i2b2_request_xml text
);

--
-- Name: qt_query_master_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_master
    ADD CONSTRAINT qt_query_master_pkey PRIMARY KEY (query_master_id);

--
-- Name: qt_idx_qm_ugid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_qm_ugid ON qt_query_master USING btree (user_id, group_id, master_type_cd);

