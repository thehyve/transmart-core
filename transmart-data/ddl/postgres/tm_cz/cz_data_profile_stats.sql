--
-- Name: cz_data_profile_stats; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_data_profile_stats (
    table_name character varying(500) NOT NULL,
    column_name character varying(500) NOT NULL,
    data_type character varying(500),
    column_length integer,
    column_precision integer,
    column_scale integer NOT NULL,
    total_count bigint,
    percentage_null real,
    null_count bigint,
    non_null_count bigint,
    distinct_count bigint,
    max_length integer,
    min_length integer,
    first_value character varying(4000),
    last_value character varying(4000),
    max_length_value character varying(4000),
    min_length_value character varying(4000),
    etl_date timestamp without time zone DEFAULT ('now'::text)::timestamp without time zone
);

