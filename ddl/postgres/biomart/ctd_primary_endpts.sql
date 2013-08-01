--
-- Name: ctd_primary_endpts; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE ctd_primary_endpts (
    ctd_study_id bigint,
    primary_type character varying(4000),
    primary_type_definition character varying(4000),
    primary_type_time_period character varying(4000),
    primary_type_change character varying(4000),
    primary_type_p_value character varying(4000),
    id bigint
);

