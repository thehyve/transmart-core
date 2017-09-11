--
-- Name: modifier_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE modifier_dimension (
    modifier_path character varying(700) NOT NULL,
    modifier_cd character varying(50) NOT NULL,
    name_char character varying(2000),
    modifier_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id bigint,
    modifier_level bigint,
    modifier_node_type character varying(10)
);

--
-- Name: modifier_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY modifier_dimension
    ADD CONSTRAINT modifier_dimension_pk PRIMARY KEY (modifier_cd);

--
-- Name: md_idx_modifier_path; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE UNIQUE INDEX md_idx_modifier_path ON modifier_dimension USING btree (modifier_path);

--
-- Name: md_idx_uploadid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX md_idx_uploadid ON modifier_dimension USING btree (upload_id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.modifier_dimension IS 'Table holds the modifiers on an observation. Used to link to highdim data and samples as well.';

COMMENT ON COLUMN modifier_dimension.modifier_cd IS 'Primary key. The code that is used to refer to the modifier from obervation_fact.';
COMMENT ON COLUMN modifier_dimension.modifier_path IS 'REQUIRED. A path that uniquely identifies a modifier.';
COMMENT ON COLUMN modifier_dimension.name_char IS 'The name of the modifier.';
