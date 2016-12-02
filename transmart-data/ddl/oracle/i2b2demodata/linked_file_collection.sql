--
-- Name: linked_file_collection; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE linked_file_collection (
    name character varying(900),
    study CHARACTER VARYING(50),
    source_system numeric(38,0) NOT NULL,
    uuid character varying(50)
);
