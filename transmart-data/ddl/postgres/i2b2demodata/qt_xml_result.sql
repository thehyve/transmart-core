--
-- Name: qt_sq_qxr_xrid; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE qt_sq_qxr_xrid
    START WITH 655
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: qt_xml_result; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_xml_result (
    xml_result_id numeric(5,0) DEFAULT nextval('qt_sq_qxr_xrid'::regclass) NOT NULL,
    result_instance_id numeric(5,0),
    xml_value character varying(4000)
);

--
-- Name: qt_xml_result_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_xml_result
    ADD CONSTRAINT qt_xml_result_pkey PRIMARY KEY (xml_result_id);

--
-- Name: qt_fk_xmlr_riid; Type: FK CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_xml_result
    ADD CONSTRAINT qt_fk_xmlr_riid FOREIGN KEY (result_instance_id) REFERENCES qt_query_result_instance(result_instance_id);

