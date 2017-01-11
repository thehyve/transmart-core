CREATE TABLE i2b2demodata.linked_file_collection (
    id integer NOT NULL,
    name character varying(900),
    study_id integer NOT NULL,
    source_system_id integer NOT NULL,
    uuid character varying(50)
);

ALTER TABLE ONLY i2b2demodata.linked_file_collection
    ADD CONSTRAINT linked_file_collection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY i2b2demodata.linked_file_collection
    ADD CONSTRAINT "SOURCE_SYSTEM_ID_ID_FK" FOREIGN KEY (source_system_id) REFERENCES i2b2demodata.storage_system(id);

ALTER TABLE ONLY i2b2demodata.linked_file_collection
    ADD CONSTRAINT "STUDY_ID_FK" FOREIGN KEY (study_id) REFERENCES i2b2demodata.study(study_num);

GRANT DELETE, UPDATE, SELECT, INSERT ON TABLE i2b2demodata.linked_file_collection TO biomart_user;
GRANT ALL ON TABLE i2b2demodata.linked_file_collection TO i2b2demodata;
GRANT ALL ON TABLE i2b2demodata.linked_file_collection TO tm_cz;