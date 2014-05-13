--
-- Type: SEQUENCE; Owner: I2B2METADATA; Name: I2B2_ID_SEQ
--
CREATE SEQUENCE i2b2_id_seq
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 445288
    CACHE 1
;

--
-- Name: i2b2; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2 (
    c_hlevel numeric(22,0) NOT NULL,
    c_fullname character varying(700) NOT NULL,
    c_name character varying(2000) NOT NULL,
    c_synonym_cd character(1) NOT NULL,
    c_visualattributes character(3) NOT NULL,
    c_totalnum numeric(22,0),
    c_basecode character varying(50),
    c_metadataxml text,
    c_facttablecolumn character varying(50) NOT NULL,
    c_tablename character varying(150) NOT NULL,
    c_columnname character varying(50) NOT NULL,
    c_columndatatype character varying(50) NOT NULL,
    c_operator character varying(10) NOT NULL,
    c_dimcode character varying(700) NOT NULL,
    c_comment text,
    c_tooltip character varying(900),
    m_applied_path character varying(700) NOT NULL,
    update_date timestamp without time zone NOT NULL,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    valuetype_cd character varying(50),
    i2b2_id bigint,
    m_exclusion_cd character varying(25),
    c_path character varying(700),
    c_symbol character varying(50)
);

--
-- Name: i2b2_c_comment_char_length_idx; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX i2b2_c_comment_char_length_idx ON i2b2 USING btree (c_comment, char_length((c_fullname)::text));

