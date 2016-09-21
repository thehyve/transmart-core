--
-- Name: ont_db_lookup; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE ont_db_lookup (
    c_domain_id character varying(255),
    c_project_path character varying(255),
    c_owner_id character varying(255),
    c_db_fullschema character varying(255),
    c_db_datasource character varying(255),
    c_db_servertype character varying(255),
    c_db_nicename character varying(255),
    c_db_tooltip character varying(255),
    c_comment text,
    c_entry_date timestamp without time zone,
    c_change_date timestamp without time zone,
    c_status_cd character(1)
);

