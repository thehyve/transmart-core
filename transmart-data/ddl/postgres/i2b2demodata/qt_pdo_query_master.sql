--
-- Name: qt_sq_pqm_qmid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_pqm_qmid
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_pdo_query_master; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_pdo_query_master (
    query_master_id numeric(5,0) DEFAULT nextval('qt_sq_pqm_qmid'::regclass) NOT NULL,
    user_id character varying(50) NOT NULL,
    group_id character varying(50) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    request_xml text,
    i2b2_request_xml text
);

--
-- Name: qt_pdo_query_master_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_pdo_query_master
    ADD CONSTRAINT qt_pdo_query_master_pkey PRIMARY KEY (query_master_id);

--
-- Name: qt_idx_pqm_ugid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_pqm_ugid ON qt_pdo_query_master USING btree (user_id, group_id);

