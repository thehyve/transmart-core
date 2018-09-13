--
-- Name: relation_type; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE i2b2demodata.relation_type (
    id SERIAL PRIMARY KEY,
    label VARCHAR(200) NOT NULL,
    description TEXT,
    symmetrical BOOLEAN,
    biological BOOLEAN
);

--
-- Name: relation_type_label_unq; Type: INDEX; Schema: i2b2demodata; Owner: -
--
CREATE UNIQUE INDEX relation_type_label_unq ON i2b2demodata.relation_type USING btree (label);

GRANT SELECT ON TABLE i2b2demodata.relation_type TO biomart_user;
GRANT ALL ON TABLE i2b2demodata.relation_type TO i2b2demodata;
GRANT ALL ON TABLE i2b2demodata.relation_type TO tm_cz;

COMMENT ON TABLE i2b2demodata.relation_type IS 'Dictionary of relations. e.g. "parent of" relation.';

COMMENT ON COLUMN i2b2demodata.relation_type.label IS 'Short unique name of the relation.';
COMMENT ON COLUMN i2b2demodata.relation_type.description IS 'Detailed description of the relation.';
COMMENT ON COLUMN i2b2demodata.relation_type.symmetrical IS 'Whether relation is symmetrical. e.g. "sibling of" is symmetrical. "parent of" is not.';
COMMENT ON COLUMN i2b2demodata.relation_type.biological IS 'Whether relation is biological. e.g. "parent of" is biological. "spouse of" is not.';
