CREATE TABLE i2b2metadata.study_dimension_descriptions (
    dimension_description_id bigint NOT NULL,
    study_id bigint NOT NULL
);

ALTER TABLE ONLY i2b2metadata.study_dimension_descriptions
    ADD CONSTRAINT study_dimension_descriptions_pkey PRIMARY KEY (study_id, dimension_description_id);

ALTER TABLE ONLY i2b2metadata.study_dimension_descriptions
    ADD CONSTRAINT fk_dimension_description_id FOREIGN KEY (dimension_description_id) REFERENCES i2b2metadata.dimension_description(id);

GRANT SELECT ON TABLE i2b2metadata.study_dimension_descriptions TO biomart_user;
GRANT ALL ON TABLE i2b2metadata.study_dimension_descriptions TO i2b2metadata;
GRANT ALL ON TABLE i2b2metadata.study_dimension_descriptions TO tm_cz;
