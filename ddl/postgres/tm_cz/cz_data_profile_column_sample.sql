--
-- Name: cz_data_profile_column_sample; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_data_profile_column_sample (
    table_name character varying(500),
    column_name character varying(500),
    value character varying(4000),
    count bigint,
    etl_date timestamp without time zone DEFAULT ('now'::text)::timestamp without time zone
);

