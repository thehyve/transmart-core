--
-- Name: archive_observation_fact; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE archive_observation_fact (
    encounter_num numeric(38,0),
    patient_num numeric(38,0),
    concept_cd character varying(50),
    provider_id character varying(50),
    start_date timestamp without time zone,
    modifier_cd character varying(100),
    instance_num numeric(18,0),
    valtype_cd character varying(50),
    tval_char character varying(255),
    nval_num numeric(18,5),
    valueflag_cd character varying(50),
    quantity_num numeric(18,5),
    units_cd character varying(50),
    end_date timestamp without time zone,
    location_cd character varying(50),
    observation_blob text,
    confidence_num numeric(18,5),
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0),
    archive_upload_id numeric(22,0)
);

--
-- Name: pk_archive_obsfact; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pk_archive_obsfact ON archive_observation_fact USING btree (encounter_num, patient_num, concept_cd, provider_id, start_date, modifier_cd, archive_upload_id);

