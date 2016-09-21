--
-- Name: custom_meta; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE custom_meta (
    c_hlevel bigint NOT NULL,
    c_fullname character varying(700) NOT NULL,
    c_name character varying(2000) NOT NULL,
    c_synonym_cd character(1) NOT NULL,
    c_visualattributes character(3) NOT NULL,
    c_totalnum bigint,
    c_basecode character varying(50),
    c_metadataxml text,
    c_facttablecolumn character varying(50) NOT NULL,
    c_tablename character varying(50) NOT NULL,
    c_columnname character varying(50) NOT NULL,
    c_columndatatype character varying(50) NOT NULL,
    c_operator character varying(10) NOT NULL,
    c_dimcode character varying(900) NOT NULL,
    c_comment text,
    c_tooltip character varying(900),
    update_date timestamp without time zone NOT NULL,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    valuetype_cd character varying(50),
    m_applied_path character varying(700) NOT NULL,
    m_exclusion_cd character varying(25),
    c_path character varying(700),
    c_symbol character varying(50)
);

--
-- Name: meta_applied_path_custom_idx; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX meta_applied_path_custom_idx ON custom_meta USING btree (m_applied_path);

--
-- Name: meta_fullname_custom_idx; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX meta_fullname_custom_idx ON custom_meta USING btree (c_fullname);

