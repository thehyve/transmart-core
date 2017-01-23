--
-- Name: observation_fact_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE observation_fact_release (
    encounter_num bigint,
    patient_num bigint,
    concept_cd character varying(50) NOT NULL,
    provider_id character varying(50) NOT NULL,
    start_date timestamp without time zone,
    modifier_cd character varying(100),
    valtype_cd character varying(50),
    tval_char character varying(255),
    nval_num double precision,
    valueflag_cd character varying(50),
    quantity_num double precision,
    units_cd character varying(50),
    end_date timestamp without time zone,
    location_cd character varying(50) NOT NULL,
    confidence_num bigint,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    observation_blob text,
    release_study character varying(100)
);

