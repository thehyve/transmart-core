--
-- sequence galaxy.status_of_export_job_seq
--

set search_path = galaxy, pg_catalog;

--
-- Name: status_of_export_job_seq; Type: SEQUENCE; Schema: galaxy; Owner: -
--
CREATE SEQUENCE status_of_export_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE galaxy.status_of_export_job_seq OWNER TO galaxy;
