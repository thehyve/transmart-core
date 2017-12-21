--
-- Name: query_diff_entry; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.query_diff_entry (
    id SERIAL PRIMARY KEY,
    query_diff_id INTEGER NOT NULL,
    object_id numeric(38,0) NOT NULL,
    change_flag character varying(7) NOT NULL
);

--
-- Name: query_diff_entry_query_diff_id_fk; Type: FK CONSTRAINT; Schema: biomart_user; Owner: -
--
ALTER TABLE ONLY biomart_user.query_diff_entry
    ADD CONSTRAINT query_diff_entry_query_diff_id_fk FOREIGN KEY (query_diff_id) REFERENCES biomart_user.query_diff(id);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_diff_entry IS 'Table stores information about specific objects deleted or added  data changes for subscribed user queries.';

COMMENT ON COLUMN biomart_user.query_diff_entry.query_diff_id IS 'Foreign key to id in query_diff table.';
COMMENT ON COLUMN biomart_user.query_diff_entry.object_id IS 'The id of the object from the set that was updated.';
COMMENT ON COLUMN biomart_user.query_diff_entry.change_flag IS 'The flag determining whether the object was added or removed from the related set';
