--
-- Name: ctd_sec_endpts; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd_sec_endpts (
    ctd_study_id bigint,
    secondary_type character varying(4000),
    secondary_type_definition character varying(4000),
    secondary_type_time_period character varying(4000),
    secondary_type_change character varying(4000),
    secondary_type_p_value character varying(4000),
    id bigint
);

