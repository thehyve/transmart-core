--
-- Name: subset; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE subset (
    subset_id bigint NOT NULL,
    description character varying(1000) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    creating_user character varying(200) NOT NULL,
    public_flag boolean DEFAULT false NOT NULL,
    deleted_flag boolean DEFAULT false NOT NULL,
    query_master_id_1 bigint NOT NULL,
    query_master_id_2 bigint,
    study character varying(200)
);

