--
-- Name: qt_analysis_plugin; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE qt_analysis_plugin (
    plugin_id numeric(10,0) NOT NULL,
    plugin_name character varying(2000),
    description character varying(2000),
    version_cd character varying(50),
    parameter_info text,
    parameter_info_xsd text,
    command_line text,
    working_folder text,
    commandoption_cd text,
    plugin_icon text,
    status_cd character varying(50),
    user_id character varying(50),
    group_id character varying(50),
    create_date timestamp without time zone,
    update_date timestamp without time zone
);

--
-- Name: analysis_plugin_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY qt_analysis_plugin
    ADD CONSTRAINT analysis_plugin_pk PRIMARY KEY (plugin_id);

--
-- Name: qt_apnamevergrp_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX qt_apnamevergrp_idx ON qt_analysis_plugin USING btree (plugin_name, version_cd, group_id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.observation_fact IS 'Not used by TranSMART. The plug-in’s metadata for an individual project is stored in this
QT_ANALYSIS_PLUGIN table.';