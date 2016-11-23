--
-- Name: subset; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE subset (
    subset_id bigint NOT NULL,
    description character varying(1000) NOT NULL,
    create_date date NOT NULL,
    creating_user character varying(200) NOT NULL,
    query_master_id_1 bigint NOT NULL,
    query_master_id_2 bigint,
    study character varying(200),
    public_flag boolean NOT NULL,
    deleted_flag boolean NOT NULL
);

