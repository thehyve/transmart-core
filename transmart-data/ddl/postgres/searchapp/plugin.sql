--
-- Name: plugin; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE plugin (
    plugin_seq bigint NOT NULL,
    name character varying(200) NOT NULL,
    plugin_name character varying(90) NOT NULL,
    has_modules character(1) DEFAULT 'N'::bpchar NOT NULL,
    has_form character(1) DEFAULT 'N'::bpchar NOT NULL,
    default_link character varying(70) NOT NULL,
    form_link character varying(70),
    form_page character varying(100),
    active character(1)
);

--
-- Name: plugin_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY plugin
    ADD CONSTRAINT plugin_pk PRIMARY KEY (plugin_seq);

