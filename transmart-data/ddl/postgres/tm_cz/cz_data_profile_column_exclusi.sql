--
-- Name: cz_data_profile_column_exclusi; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_data_profile_column_exclusi (
    table_name character varying(500) NOT NULL,
    column_name character varying(500) NOT NULL,
    exclusion_reason character varying(2000),
    etl_date timestamp without time zone DEFAULT ('now'::text)::timestamp without time zone
);

