--
-- Name: plugin_module_backup; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE plugin_module_backup (
    module_seq bigint NOT NULL,
    plugin_seq bigint NOT NULL,
    name character varying(70) NOT NULL,
    params text NOT NULL,
    version character varying(10) NOT NULL,
    active character(1) NOT NULL,
    has_form character(1) NOT NULL,
    form_link character varying(90),
    form_page character varying(90),
    module_name character varying(50) NOT NULL,
    category character varying(50)
);

