--
-- Name: de_pathway; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_pathway (
    name character varying(300),
    description character varying(510),
    id bigint NOT NULL,
    type character varying(100),
    source character varying(100),
    externalid character varying(100),
    pathway_uid character varying(200),
    user_id bigint,
    PRIMARY KEY (id)
);

