--
-- Name: dimloader; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE dimloader (
    c_hlevel bigint,
    c_fullname character varying(900),
    c_name character varying(2000),
    c_synonym_cd character(1),
    c_visualattributes character(3),
    c_totalnum bigint,
    c_basecode character varying(50),
    c_metadataxml text,
    c_facttablecolumn character varying(50),
    c_tablename character varying(50),
    c_columnname character varying(50),
    c_columndatatype character varying(50),
    c_operator character varying(10),
    c_dimcode character varying(900),
    c_comment text,
    c_tooltip character varying(900),
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    valuetype_cd character varying(50)
);

