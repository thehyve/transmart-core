--
-- Name: query_set_diff; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.query_set_diff (
    id SERIAL PRIMARY KEY,
    query_set_id INTEGER NOT NULL,
    object_id numeric(38,0) NOT NULL,
    change_flag character varying(7) NOT NULL
);

--
-- Name: query_set_diff_query_set_id_fk; Type: FK CONSTRAINT; Schema: biomart_user; Owner: -
--
ALTER TABLE ONLY biomart_user.query_set_diff
    ADD CONSTRAINT query_set_diff_query_set_id_fk FOREIGN KEY (query_set_id) REFERENCES biomart_user.query_set(id);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_set_diff IS 'Table stores information about specific objects, deleted or added to the subscribed user query related set.';

COMMENT ON COLUMN biomart_user.query_set_diff.query_set_id IS 'Foreign key to id in query_set table.';
COMMENT ON COLUMN biomart_user.query_set_diff.object_id IS 'The id of the object from the set that was updated, e.g. id of an i2b2demodata.patient_dimension instance.';
COMMENT ON COLUMN biomart_user.query_set_diff.change_flag IS 'The flag determining whether the object was added or removed from the related set';

