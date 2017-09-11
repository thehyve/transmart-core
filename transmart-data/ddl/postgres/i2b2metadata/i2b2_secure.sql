--
-- Name: i2b2_secure; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2_secure (
    c_hlevel numeric(22,0),
    c_fullname character varying(900) NOT NULL,
    c_name character varying(2000),
    c_synonym_cd character(1),
    c_visualattributes character(3),
    c_totalnum numeric(22,0),
    c_basecode character varying(450),
    c_metadataxml text,
    c_facttablecolumn character varying(50),
    c_tablename character varying(150),
    c_columnname character varying(50),
    c_columndatatype character varying(50),
    c_operator character varying(10),
    c_dimcode character varying(900),
    c_comment text,
    c_tooltip character varying(900),
    m_applied_path character varying(700),
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    valuetype_cd character varying(50),
    m_exclusion_cd character varying(25),
    c_path character varying(900),
    c_symbol character varying(50),
    i2b2_id numeric(18,0),
    secure_obj_token character varying(50)
);

--
-- Name: i2b2_secure_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2_secure
    ADD CONSTRAINT i2b2_secure_pkey PRIMARY KEY (c_fullname);

--
-- Name: idx_i2b2_secure_level_fullname; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX idx_i2b2_secure_level_fullname ON i2b2_secure USING btree (c_hlevel, c_fullname);

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.i2b2_secure IS 'The same as i2b2, but with an added security token.';

COMMENT ON COLUMN i2b2_secure.c_hlevel IS 'Number that represents the depth of the node. 0 for root.';
COMMENT ON COLUMN i2b2_secure.c_fullname IS 'Full path to the node. E.g., \Vital Signs\Heart Rate\.';
COMMENT ON COLUMN i2b2_secure.c_name IS 'Name of the node. E.g., Heart Rate.';
COMMENT ON COLUMN i2b2_secure.c_basecode IS 'Code that represents node. E.g., VSIGN:HR. Not used.';
COMMENT ON COLUMN i2b2_secure.c_visualattributes IS 'Visual attributes describing how a node should be displayed. Can have three characters at maximum. See OntologyTerm#VisualAttributes for documentation on the values.';
COMMENT ON COLUMN i2b2_secure.c_metadataxml IS 'Metadata encoded as XML.';
COMMENT ON COLUMN i2b2_secure.c_facttablecolumn IS 'Column of observation_fact corresponding with c_columnname.';
COMMENT ON COLUMN i2b2_secure.c_tablename IS 'Table of the dimension referred to by this node.';
COMMENT ON COLUMN i2b2_secure.c_columnname IS 'Column of the table of the dimension referred to by this node';
COMMENT ON COLUMN i2b2_secure.c_operator IS 'Operator. E.g., like, =';
COMMENT ON COLUMN i2b2_secure.c_dimcode IS 'Refers to a dimension element, linked to observations.';
COMMENT ON COLUMN i2b2_secure.c_comment IS 'Meant for comments, not for storing study based security tokens.';

COMMENT ON COLUMN i2b2_secure.secure_obj_token IS 'Token encoding access the node. Refers to bio_data_unique_id in searchapp.search_secure_object. E.g., PUBLIC or EXP:SCSCP.';
