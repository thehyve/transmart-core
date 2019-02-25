--
-- Name: query_set_instance; Type: TABLE; Schema: biomart_user; Owner: -
-- DEPRECATED! user queries related functionality has been moved to a gb-backend application
--
CREATE TABLE biomart_user.query_set_instance (
    id SERIAL PRIMARY KEY,
    query_set_id INTEGER NOT NULL,
    object_id numeric(38,0) NOT NULL
);

--
-- Name: query_set_instance_query_set_id_fk; Type: FK CONSTRAINT; Schema: biomart_user; Owner: -
--
ALTER TABLE ONLY biomart_user.query_set_instance
    ADD CONSTRAINT query_set_instance_query_set_id_fk FOREIGN KEY (query_set_id) REFERENCES biomart_user.query_set(id);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_set_instance IS 'Table stores information about specific instances of the query set. DEPRECATED! This table has been moved to a gb-backend application.';

COMMENT ON COLUMN biomart_user.query_set_instance.query_set_id IS 'Foreign key to id in query_set table.';
COMMENT ON COLUMN biomart_user.query_set_instance.object_id IS 'The id of the object that is being represented by the set instance, e.g. id of an i2b2demodata.patient_dimension instance.';

