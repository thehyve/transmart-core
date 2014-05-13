--
-- Name: de_snp_calls_by_gsm; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_calls_by_gsm (
    gsm_num character varying(100),
    trial_name character varying(20),
    patient_num bigint,
    snp_name character varying(100),
    snp_calls character varying(4)
);

