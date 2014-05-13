--
-- Type: SEQUENCE; Owner: I2B2DEMODATA; Name: SEQ_ENCOUNTER_NUM
--
CREATE SEQUENCE seq_encounter_num
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 9571119
    CACHE 1
;

--
-- Name: observation_fact; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE observation_fact (
    encounter_num bigint,
    patient_num bigint NOT NULL,
    concept_cd character varying(50) NOT NULL,
    provider_id character varying(50) NOT NULL,
    start_date timestamp without time zone,
    modifier_cd character varying(100) NOT NULL,
    valtype_cd character varying(50),
    tval_char character varying(255),
    nval_num double precision,
    valueflag_cd character varying(50),
    quantity_num numeric(18,5),
    units_cd character varying(50),
    end_date timestamp without time zone,
    location_cd character varying(50),
    confidence_num bigint,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    sample_cd character varying(200),
    observation_blob text,
    instance_num bigint
);

--
-- Name: fact_modifier_patient; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX fact_modifier_patient ON observation_fact USING btree (modifier_cd, patient_num);

