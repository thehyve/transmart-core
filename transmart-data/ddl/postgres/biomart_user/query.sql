--
-- Name: query; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.query (
    id SERIAL PRIMARY KEY,
    name character varying(1000) NOT NULL,
    username character varying(50) NOT NULL,
    patients_query text,
    observations_query text,
    api_version character varying(25),
    bookmarked boolean,
    deleted boolean,
    create_date timestamp without time zone,
    update_date timestamp without time zone
);

--
-- Name: query_user; Type: INDEX; Schema: biomart_user; Owner: -
--
CREATE INDEX query_username_deleted ON biomart_user.query USING btree (username, deleted);

--
-- Table documentation
--
COMMENT ON TABLE biomart_user.query IS 'Storage for patients and observations queries to support front end functionality.';

COMMENT ON COLUMN biomart_user.query.name IS 'The query name.';
COMMENT ON COLUMN biomart_user.query.username IS 'The username of the user that created the query.';
COMMENT ON COLUMN biomart_user.query.patients_query IS 'The patient selection part of the query.';
COMMENT ON COLUMN biomart_user.query.observations_query IS 'The observation selection part of the query.';
COMMENT ON COLUMN biomart_user.query.api_version IS 'The version of the API the query was intended for.';
COMMENT ON COLUMN biomart_user.query.bookmarked IS 'Flag to indicate if the user has bookmarked the query.';
COMMENT ON COLUMN biomart_user.query.deleted IS 'Flag to indicate if the query has been deleted.';
