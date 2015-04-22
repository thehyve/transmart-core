--
-- Name: modifier_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE modifier_dimension (
    modifier_path character varying(700) NOT NULL,
    modifier_cd character varying(50),
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
-- Name: modifier_dimension_modifier_cd_uq; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY modifier_dimension
    ADD CONSTRAINT modifier_dimension_modifier_cd_uq UNIQUE (modifier_cd);

--
-- Name: modifier_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY modifier_dimension
    ADD CONSTRAINT modifier_dimension_pk PRIMARY KEY (modifier_path);

--
-- Name: md_idx_uploadid; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX md_idx_uploadid ON modifier_dimension USING btree (upload_id);

