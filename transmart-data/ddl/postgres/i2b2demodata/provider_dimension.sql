--
-- Name: provider_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE provider_dimension (
    provider_id character varying(50) NOT NULL,
    provider_path character varying(700) NOT NULL,
    name_char character varying(850),
    provider_blob text,
    update_date timestamp without time zone,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    upload_id numeric(38,0)
);

--
-- Name: provider_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY provider_dimension
    ADD CONSTRAINT provider_dimension_pk PRIMARY KEY (provider_path, provider_id);

--
-- Name: pd_idx_name_char; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX pd_idx_name_char ON provider_dimension USING btree (provider_id, name_char);

--
-- Name: prod_uploadid_idx; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE INDEX prod_uploadid_idx ON provider_dimension USING btree (upload_id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.provider_dimension IS 'Table that holds providers, e.g., physicians.';

COMMENT ON COLUMN provider_dimension.provider_id IS 'Primary key.';
COMMENT ON COLUMN provider_dimension.provider_path IS 'Primary key. A path that identifies a provider.';
COMMENT ON COLUMN provider_dimension.name_char IS 'The name of the provider.';
