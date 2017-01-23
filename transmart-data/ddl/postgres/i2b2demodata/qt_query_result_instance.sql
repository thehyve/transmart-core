--
-- Name: qt_sq_qri_qriid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qri_qriid
    START WITH 28714
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_query_result_instance; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_query_result_instance (
    result_instance_id numeric(5,0) DEFAULT nextval('qt_sq_qri_qriid'::regclass) NOT NULL,
    query_instance_id numeric(5,0),
    result_type_id numeric(3,0) NOT NULL,
    set_size numeric(10,0),
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone,
    delete_flag character varying(3),
    status_type_id numeric(3,0) NOT NULL,
    message text,
    description character varying(200),
    real_set_size numeric(10,0),
    obfusc_method character varying(500)
);

--
-- Name: qt_query_result_instance_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_result_instance
    ADD CONSTRAINT qt_query_result_instance_pkey PRIMARY KEY (result_instance_id);

--
-- Name: qt_fk_qri_rid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_result_instance
    ADD CONSTRAINT qt_fk_qri_rid FOREIGN KEY (query_instance_id) REFERENCES qt_query_instance(query_instance_id);

--
-- Name: qt_fk_qri_rtid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_result_instance
    ADD CONSTRAINT qt_fk_qri_rtid FOREIGN KEY (result_type_id) REFERENCES qt_query_result_type(result_type_id);

--
-- Name: qt_fk_qri_stid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_query_result_instance
    ADD CONSTRAINT qt_fk_qri_stid FOREIGN KEY (status_type_id) REFERENCES qt_query_status_type(status_type_id);

