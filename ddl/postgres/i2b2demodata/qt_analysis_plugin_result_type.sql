--
-- Name: qt_analysis_plugin_result_type; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_analysis_plugin_result_type (
    plugin_id numeric(10,0) NOT NULL,
    result_type_id numeric(10,0) NOT NULL
);

--
-- Name: analysis_plugin_result_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_analysis_plugin_result_type
    ADD CONSTRAINT analysis_plugin_result_pk PRIMARY KEY (plugin_id, result_type_id);

