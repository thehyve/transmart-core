--
-- Name: query_set; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.query_set (
    id SERIAL PRIMARY KEY,
    query_id INTEGER NOT NULL,
    set_size numeric(10,0),
    set_type character varying(25) NOT NULL,
    create_date timestamp without time zone NOT NULL
);

--
-- Name: query_set_query_id_fk; Type: FK CONSTRAINT; Schema: biomart_user; Owner: -
--
ALTER TABLE ONLY biomart_user.query_set
  ADD CONSTRAINT query_set_query_id_fk FOREIGN KEY (query_id) REFERENCES biomart_user.query(id);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query_set IS 'Table stores information about data sets for user queries.';

COMMENT ON COLUMN biomart_user.query_set.query_id IS 'Foreign key to id in a query table.';
COMMENT ON COLUMN biomart_user.query_set.set_size IS 'The size of the set';
COMMENT ON COLUMN biomart_user.query_set.set_type IS 'The type of the set: [Patient | Sample].';
COMMENT ON COLUMN biomart_user.query_set.create_date IS 'The date of the set creation.';
