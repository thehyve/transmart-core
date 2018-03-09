--
-- Name: concept_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE concept_dimension (
    concept_cd character varying(50) NOT NULL,
    concept_path character varying(700) NOT NULL,
    name_char character varying(2000),
    concept_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    table_name character varying(255)
);

--
-- Name: concept_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY concept_dimension
    ADD CONSTRAINT concept_dimension_pk PRIMARY KEY (concept_path);

--
-- Name: concept_dimension_concept_cd_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX concept_dimension_concept_cd_idx ON concept_dimension USING btree (concept_cd);

--
-- Name: concept_id; Type: SEQUENCE; Schema: i2b2demodata; Owner: -
--
CREATE SEQUENCE concept_id
    START WITH 1341004
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.concept_dimension IS 'Table contains the concepts that classify observations.';

COMMENT ON COLUMN concept_dimension.concept_path IS 'Primary key. The path that uniquely identifies a concept.';
COMMENT ON COLUMN concept_dimension.concept_cd IS 'REQUIRED. The code that is used to refer to the concept from observation_fact.';
COMMENT ON COLUMN concept_dimension.name_char IS 'REQUIRED. The name of the concept.';
