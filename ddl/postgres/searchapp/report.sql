--
-- Name: report; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE report (
    report_id bigint,
    name character varying(200),
    description character varying(1000),
    creatinguser character varying(200),
    public_flag character varying(20), --oracle was character(1) in postgres
    create_date timestamp without time zone,
    study character varying(200)
);

