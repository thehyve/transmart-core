CREATE SEQUENCE i2b2metadata.dimension_description_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE i2b2metadata.dimension_description (
    id integer NOT NULL,
    density character varying(255),
    modifier_code character varying(255),
    value_type character varying(50),
    name character varying(255) NOT NULL,
    packable character varying(255),
    size_cd character varying(255)
);

ALTER TABLE ONLY i2b2metadata.dimension_description ALTER COLUMN id SET DEFAULT nextval('i2b2metadata.dimension_description_id_seq'::regclass);

ALTER TABLE ONLY i2b2metadata.dimension_description
    ADD CONSTRAINT dimension_description_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX dimension_description_unique_name ON i2b2metadata.dimension_description USING btree (name);

ALTER SEQUENCE i2b2metadata.dimension_description_id_seq OWNED BY i2b2metadata.dimension_description.id;

GRANT SELECT ON TABLE i2b2metadata.dimension_description TO biomart_user;
GRANT ALL ON TABLE i2b2metadata.dimension_description TO i2b2metadata;
GRANT ALL ON TABLE i2b2metadata.dimension_description TO tm_cz;
