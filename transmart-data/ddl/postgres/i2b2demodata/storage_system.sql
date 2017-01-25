--
-- Name: storage_system; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE storage_system (
    id integer NOT NULL,
    name character varying(50),
    system_type character varying(50),
    url character varying(900),
    system_version character varying(50),
    single_file_collections boolean
);

--
-- Name: storage_system_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY storage_system
    ADD CONSTRAINT storage_system_pkey PRIMARY KEY (id);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.storage_system IS 'Registered storage instances.';

COMMENT ON COLUMN storage_system.name IS 'Name of the system.';
COMMENT ON COLUMN storage_system.system_type IS 'Storage system type. e.g. Arvados';
COMMENT ON COLUMN storage_system.url IS 'URL of the instance.';
COMMENT ON COLUMN storage_system.system_version IS 'Version of the storage system, used for formulating requests by the frontend.';
COMMENT ON COLUMN storage_system.single_file_collections IS 'True for systems where collection=one file, false for those where collection=file tree (like Arvados Keep)';