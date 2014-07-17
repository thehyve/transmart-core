--
-- Name: search_auth_principal; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_auth_principal (
    id bigint NOT NULL,
    principal_type character varying(255),
    date_created timestamp without time zone NOT NULL,
    description character varying(255),
    last_updated timestamp without time zone NOT NULL,
    name character varying(255),
    unique_id character varying(255),
    enabled boolean
);

--
-- Name: pk_search_principal; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_principal
    ADD CONSTRAINT pk_search_principal PRIMARY KEY (id);
