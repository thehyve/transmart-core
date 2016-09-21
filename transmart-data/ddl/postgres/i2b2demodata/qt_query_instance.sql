--
-- Name: qt_sq_qi_qiid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qi_qiid
    START WITH 28734
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_query_instance; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_query_instance (
    query_instance_id numeric(5,0) DEFAULT nextval('qt_sq_qi_qiid'::regclass) NOT NULL,
    query_master_id numeric(5,0),
    user_id character varying(50) NOT NULL,
    group_id character varying(50) NOT NULL,
    batch_mode character varying(50),
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone,
    delete_flag character varying(3),
    status_type_id numeric(5,0),
    message text
);

--
-- Name: qt_query_instance_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_instance
    ADD CONSTRAINT qt_query_instance_pkey PRIMARY KEY (query_instance_id);

--
-- Name: qt_idx_qi_mstartid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_qi_mstartid ON qt_query_instance USING btree (query_master_id, start_date);

--
-- Name: qt_idx_qi_ugid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_idx_qi_ugid ON qt_query_instance USING btree (user_id, group_id);

--
-- Name: qt_fk_qi_mid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_instance
    ADD CONSTRAINT qt_fk_qi_mid FOREIGN KEY (query_master_id) REFERENCES qt_query_master(query_master_id);

--
-- Name: qt_fk_qi_stid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_instance
    ADD CONSTRAINT qt_fk_qi_stid FOREIGN KEY (status_type_id) REFERENCES qt_query_status_type(status_type_id);

