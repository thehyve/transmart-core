--
-- Name: patient_dimension_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE patient_dimension_release (
    patient_num bigint,
    vital_status_cd character varying(50),
    birth_date timestamp without time zone,
    death_date timestamp without time zone,
    sex_cd character varying(50),
    age_in_years_num bigint,
    language_cd character varying(50),
    race_cd character varying(50),
    marital_status_cd character varying(50),
    religion_cd character varying(50),
    zip_cd character varying(50),
    statecityzip_path character varying(700),
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    patient_blob text,
    release_study character varying(50)
);

