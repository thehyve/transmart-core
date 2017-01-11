CREATE TABLE i2b2demodata.supported_workflow (
    id integer NOT NULL,
    name character varying(500),
    description character varying(900),
    uuid character varying(50) NOT NULL,
    arvados_instance_url character varying(900),
    arvados_version character varying(50),
    default_params text
);

ALTER TABLE ONLY i2b2demodata.supported_workflow
    ADD CONSTRAINT supported_workflow_pkey PRIMARY KEY (id);

GRANT DELETE, UPDATE, SELECT, INSERT ON TABLE i2b2demodata.supported_workflow TO biomart_user;
GRANT ALL ON TABLE i2b2demodata.supported_workflow TO i2b2demodata;
GRANT ALL ON TABLE i2b2demodata.supported_workflow TO tm_cz;