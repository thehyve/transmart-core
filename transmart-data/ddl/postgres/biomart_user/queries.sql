--
-- Name: queries; Type: TABLE; Schema: biomart_user; Owner: -
--
CREATE TABLE biomart_user.queries (
    id SERIAL PRIMARY KEY,
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
-- Name: queries_user; Type: INDEX; Schema: biomart_user; Owner: -
--
CREATE INDEX queries_user ON biomart_user.queries USING btree (username);
