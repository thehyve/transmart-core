--
-- Name: plugin_module; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE plugin_module (
    module_seq bigint NOT NULL,
    plugin_seq bigint NOT NULL,
    name character varying(70) NOT NULL,
    params text,
    version character varying(10) DEFAULT 0.1,
    active character(1) DEFAULT 'Y'::bpchar,
    has_form character(1) DEFAULT 'N'::bpchar,
    form_link character varying(90),
    form_page character varying(90),
    module_name character varying(50) NOT NULL,
    category character varying(50)
);

--
-- Name: plugin_module_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY plugin_module
    ADD CONSTRAINT plugin_module_pk PRIMARY KEY (module_seq);

--
-- Name: plugin_module_plugin_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY plugin_module
    ADD CONSTRAINT plugin_module_plugin_fk1 FOREIGN KEY (plugin_seq) REFERENCES plugin(plugin_seq);

