--
-- Name: search_app_access_log; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_app_access_log (
    id bigint,
    access_time timestamp without time zone,
    event character varying(255),
    request_url character varying(255),
    user_name character varying(255),
    event_message text
);

