--
-- Name: linked_file_collection; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE linked_file_collection (
    id integer NOT NULL,
    name character varying(900),
    study_id integer NOT NULL,
    source_system_id integer NOT NULL,
    uuid character varying(50)
);

--
-- Name: linked_file_collection_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY linked_file_collection
    ADD CONSTRAINT linked_file_collection_pkey PRIMARY KEY (id);
--
-- Name: SOURCE_SYSTEM_ID_ID_FK; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE i2b2demodata.linked_file_collection ADD CONSTRAINT "SOURCE_SYSTEM_ID_ID_FK" FOREIGN KEY ("source_system_id")
 REFERENCES i2b2demodata.storage_system (id);
--
-- Name: STUDY_ID_FK; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE i2b2demodata.linked_file_collection ADD CONSTRAINT "STUDY_ID_FK" FOREIGN KEY ("study_id")
 REFERENCES i2b2demodata.study (study_num);

--
-- add documentation
--
COMMENT ON TABLE i2b2demodata.linked_file_collection IS 'Stores universally unique identifier to the collection of files (a folder) on an external file system (e.g. Arvados Keep)';

COMMENT ON COLUMN linked_file_collection.name IS 'Name of the collection.';
COMMENT ON COLUMN linked_file_collection.study_id IS 'Points to the study the file associated with.';
COMMENT ON COLUMN linked_file_collection.source_system_id IS 'Foreign key. Specifies storage instance (e.g. a Arvados Keep server).';
COMMENT ON COLUMN linked_file_collection.source_system_id IS 'The universally unique identifier on the storage system.';