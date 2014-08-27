--
-- Name: table_access; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE table_access (
    c_table_cd character varying(65) NOT NULL,
    c_table_name character varying(50) NOT NULL,
    c_protected_access character(1),
    c_hlevel numeric(22,0) NOT NULL,
    c_fullname character varying(700) NOT NULL,
    c_name character varying(2000) NOT NULL,
    c_synonym_cd character(1) NOT NULL,
    c_visualattributes character(3) NOT NULL,
    c_totalnum numeric(22,0),
    c_basecode character varying(50),
    c_metadataxml text,
    c_facttablecolumn character varying(50) NOT NULL,
    c_dimtablename character varying(50) NOT NULL,
    c_columnname character varying(50) NOT NULL,
    c_columndatatype character varying(50) NOT NULL,
    c_operator character varying(10) NOT NULL,
    c_dimcode character varying(700) NOT NULL,
    c_comment text,
    c_tooltip character varying(900),
    c_entry_date timestamp without time zone,
    c_change_date timestamp without time zone,
    c_status_cd character(1),
    valuetype_cd character varying(50)
);

--
-- Name: table_access_pk; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY table_access
    ADD CONSTRAINT table_access_pk PRIMARY KEY (c_table_cd);

