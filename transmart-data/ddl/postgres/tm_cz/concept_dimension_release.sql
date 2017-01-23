--
-- Name: concept_dimension_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE concept_dimension_release (
    concept_cd character varying(50) NOT NULL,
    concept_path character varying(700) NOT NULL,
    name_char character varying(2000),
    concept_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    table_name character varying(255),
    release_study character varying(50)
);

