--
-- Name: query_diff; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.query_diff (
    id SERIAL PRIMARY KEY,
    query_id INTEGER NOT NULL,
    set_id numeric(38,0) NOT NULL,
    set_type character varying(25) NOT NULL,
    date timestamp without time zone NOT NULL
);

--
-- Name: query_diff_query_id_fk; Type: FK CONSTRAINT; Schema: biomart_user; Owner: -
--
ALTER TABLE ONLY biomart_user.query_diff
  ADD CONSTRAINT query_diff_query_id_fk FOREIGN KEY (query_id) REFERENCES biomart_user.query(id);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_diff IS 'Table stores information about data changes for subscribed user queries.';

COMMENT ON COLUMN biomart_user.query_diff.query_id IS 'Foreign key to id in query table.';
COMMENT ON COLUMN biomart_user.query_diff.set_id IS 'The id of the set of objects that the data change relates to';
COMMENT ON COLUMN biomart_user.query_diff.set_type IS 'The type of set: [Patient | Sample].';
COMMENT ON COLUMN biomart_user.query_diff.date IS 'The date of the data change detection.';
