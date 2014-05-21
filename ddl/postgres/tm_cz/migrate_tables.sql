--
-- Name: migrate_tables; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE migrate_tables (
    data_type character varying(50),
    table_owner character varying(50),
    table_name character varying(50),
    study_specific character(1),
    where_clause character varying(2000),
    insert_seq integer,
    stage_table_name character varying(100),
    rebuild_index character(1),
    delete_seq integer
);

