CREATE TABLE i2b2demodata.storage_system (
    id integer NOT NULL,
    name character varying(50),
    system_type character varying(50),
    url character varying(900),
    system_version character varying(50),
    single_file_collections boolean
);

ALTER TABLE ONLY i2b2demodata.storage_system
    ADD CONSTRAINT storage_system_pkey PRIMARY KEY (id);