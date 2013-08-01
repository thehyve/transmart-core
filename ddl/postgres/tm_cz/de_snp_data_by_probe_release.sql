--
-- Name: de_snp_data_by_probe_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE de_snp_data_by_probe_release (
    snp_data_by_probe_id bigint,
    probe_id bigint,
    probe_name character varying(255),
    snp_id bigint,
    snp_name character varying(255),
    trial_name character varying(255),
    data_by_probe text,
    release_study character varying(200)
);

